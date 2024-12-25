package com.anthonybturner.cinemapostersanywhere.interfaces;

import android.content.Intent;

public interface TimerUpdateListener {
    void onTimerUpdate(long durationInMillis, boolean status, boolean isArenas);
    void onTimerFinish(boolean status);
    void onTimerStarted(Intent intent, boolean status, boolean isArenas);
    void onPlayerStatsTimerStarted(Intent intent, boolean status);

}