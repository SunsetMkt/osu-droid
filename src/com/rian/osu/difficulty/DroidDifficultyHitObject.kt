package com.rian.osu.difficulty

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.*
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Represents a [HitObject] with additional information for osu!droid difficulty calculation.
 */
class DroidDifficultyHitObject(
    /**
     * The [HitObject] that this [DroidDifficultyHitObject] wraps.
     */
    obj: HitObject,

    /**
     * The [HitObject] that occurs before [obj].
     */
    lastObj: HitObject?,

    /**
     * The [HitObject] that occurs before [lastObj].
     */
    lastLastObj: HitObject?,

    /**
     * The clock rate being calculated.
     */
    clockRate: Double,

    /**
     * Other hit objects in the beatmap, including this hit object.
     */
    difficultyHitObjects: MutableList<DroidDifficultyHitObject>,

    /**
     * The index of this hit object in the list of all hit objects.
     *
     * This is one less than the actual index of the hit object in the beatmap.
     */
    index: Int,

    /**
     * The full great window of the hit object.
     */
    greatWindow: Double
) : DifficultyHitObject(obj, lastObj, lastLastObj, clockRate, difficultyHitObjects, index, greatWindow) {
    override val mode = GameMode.Droid
    override val maximumSliderRadius = NORMALIZED_RADIUS * 2

    override val scalingFactor: Float
        get() {
            val radius = obj.difficultyRadius.toFloat()

            // We will scale distances by this factor, so we can assume a uniform CircleSize among beatmaps.
            var scalingFactor = NORMALIZED_RADIUS / radius

            // High circle size (small CS) bonus
            if (radius < 70) {
                scalingFactor *= 1 + ((70 - radius) / 50).pow(2)
            }

            return scalingFactor
        }

    /**
     * The note density of the [HitObject].
     */
    @JvmField
    var noteDensity = 1.0

    /**
     * The overlapping factor of the [HitObject].
     *
     * This is used to scale visual skill.
     */
    @JvmField
    var overlappingFactor = 0.0

    /**
     * Adjusted preempt time of the [HitObject], taking speed multiplier into account.
     */
    @JvmField
    val timePreempt = obj.timePreempt / clockRate

    override fun computeProperties(clockRate: Double, objects: List<HitObject>) {
        super.computeProperties(clockRate, objects)

        setVisuals(clockRate, objects)
    }

    override fun previous(backwardsIndex: Int) = difficultyHitObjects.getOrNull(index - backwardsIndex)

    override fun next(forwardsIndex: Int) = difficultyHitObjects.getOrNull(index + forwardsIndex + 2)

    /**
     * Determines whether this [DroidDifficultyHitObject] is considered overlapping with the [DroidDifficultyHitObject]
     * before it.
     *
     * Keep in mind that "overlapping" in this case is overlapping to the point where both [DroidDifficultyHitObject]s
     * can be hit with just a single tap in osu!droid.
     *
     * @param considerDistance Whether to consider the distance between both [DroidDifficultyHitObject]s.
     * @returns Whether the [DroidDifficultyHitObject] is considered overlapping.
     */
    fun isOverlapping(considerDistance: Boolean): Boolean {
        if (obj is Spinner) {
            return false
        }

        val previous = previous(0)

        if (previous == null || previous.obj is Spinner) {
            return false
        }

        if (deltaTime >= 5) {
            return false
        }

        if (considerDistance) {
            val position = obj.difficultyStackedPosition
            var distance = previous.obj.getDifficultyStackedEndPosition().getDistance(position)

            if (previous.obj is Slider && previous.obj.lazyEndPosition != null) {
                distance = min(
                    distance,
                    previous.obj.lazyEndPosition!!.getDistance(position)
                )
            }

            return distance <= 2 * obj.difficultyRadius
        }

        return true
    }

    private fun setVisuals(clockRate: Double, objects: List<HitObject>) {
        // We'll have two visible object lists. The first list contains objects before the current object starts in a
        // reversed order, while the second list contains objects after the current object ends.
        // For overlapping factor, we also need to consider objects that are prior to the current object.
        val prevVisibleObjects = mutableListOf<HitObject>()
        val nextVisibleObjects = mutableListOf<HitObject>()

        for (i in index + 2 until objects.size) {
            val o = objects[i]

            if (o is Spinner) {
                continue
            }

            if (o.startTime > obj.getEndTime() + obj.timePreempt) {
                break
            }

            nextVisibleObjects.add(o)
        }

        for (i in 0 until index) {
            val prev = previous(i) ?: continue

            if (prev.obj is Spinner) {
                continue
            }

            if (prev.obj.startTime >= obj.startTime) {
                continue
            }

            if (prev.obj.startTime < obj.startTime - obj.timePreempt) {
                break
            }

            prevVisibleObjects.add(prev.obj)
        }

        for (hitObject in prevVisibleObjects) {
            val distance = obj.difficultyStackedPosition.getDistance(hitObject.getDifficultyStackedEndPosition())
            val deltaTime = startTime - hitObject.getEndTime() / clockRate

            applyToOverlappingFactor(distance, deltaTime)
        }

        for (hitObject in nextVisibleObjects) {
            val distance = hitObject.difficultyStackedPosition.getDistance(obj.getDifficultyStackedEndPosition())

            // We are not using clock rate adjusted delta time here for note density calculation.
            val deltaTime = hitObject.startTime - obj.getEndTime()

            if (deltaTime >= 0) {
                noteDensity += 1 - deltaTime / obj.timePreempt
            }

            // But for overlapping factor, we need to use clock rate adjusted delta time.
            applyToOverlappingFactor(distance, deltaTime / clockRate)
        }
    }

    private fun applyToOverlappingFactor(distance: Float, deltaTime: Double) {
        // Penalize objects that are too close to the object in both distance
        // and delta time to prevent stream maps from being overweighted.
        overlappingFactor += max(
            0.0,
            1 - distance / (2.5 * obj.difficultyRadius)
        ) * 7.5 / (1 + exp(0.15 * (max(deltaTime, MIN_DELTA_TIME) - 75)))
    }
}