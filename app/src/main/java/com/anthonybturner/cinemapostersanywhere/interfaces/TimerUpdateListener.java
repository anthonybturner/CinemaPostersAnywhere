package com.anthonybturner.cinemapostersanywhere.interfaces;

import android.content.Intent;

public interface TimerUpdateListener {
    void onTimerUpdate(long durationInMillis, boolean isRanked, boolean isArenas);
    void onTimerFinish(boolean isRanked);
    void onTimerStarted(Intent intent, boolean isRanked, boolean isArenas);
    void onPlayerStatsTimerStarted(Intent intent, boolean isRanked);

}