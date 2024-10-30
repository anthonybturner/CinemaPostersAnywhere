package com.anthonybturner.cinemapostersanywhere.interfaces;

public interface TimerUpdateListener {
    void onTimerUpdate(long durationInMillis, boolean isRanked, boolean isArenas);
    void onTimerFinish(boolean isRanked);
    void onTimerStarted();
}