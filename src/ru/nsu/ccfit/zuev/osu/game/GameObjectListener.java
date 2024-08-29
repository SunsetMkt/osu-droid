package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.rian.osu.beatmap.hitobject.HitObject;
import com.rian.osu.beatmap.hitobject.HitSampleInfo;

import java.util.BitSet;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public interface GameObjectListener {

    int SLIDER_START = 1, SLIDER_REPEAT = 2, SLIDER_END = 3, SLIDER_TICK = 4;

    void onCircleHit(int id, float accuracy, PointF pos, boolean endCombo, byte forcedScore, RGBColor color);

    void onSliderHit(int id, int score, PointF start, PointF end,
                     boolean endCombo, RGBColor color, int type);

    void onSliderEnd(int id, int accuracy, BitSet tickSet);

    void onSpinnerHit(int id, int score, boolean endCombo, int totalScore);

    void playSamples(HitObject obj);

    void playAuxiliarySamples(HitObject obj);

    void playSample(HitSampleInfo sample, boolean loop);

    void stopAuxiliarySamples(HitObject obj);

    void stopSample(HitSampleInfo sample);

    void addObject(GameObject object);

    void removeObject(GameObject object);

    void addPassiveObject(GameObject object);

    void removePassiveObject(GameObject object);

    PointF getMousePos(int index);

    boolean isMouseDown(int index);

    boolean isMousePressed(GameObject object, int index);

    double downFrameOffset(int index);

    int getCursorsCount();

    void registerAccuracy(double acc);
    
    void updateAutoBasedPos(float pX, float pY);

    void onTrackingSliders(boolean isTrackingSliders);

    void onUpdatedAutoCursor(float pX, float pY);
}