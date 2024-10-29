package com.anthonybturner.cinemapostersanywhere.interfaces;

public interface TimerUpdateListener {
    void onTimerUpdate(long durationInMillis);
    void onTimerFinish();
    void onTimerStarted();
}