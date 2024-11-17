package com.anthonybturner.cinemapostersanywhere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class NowPlayingReceiver extends BroadcastReceiver {
    private final MovieActivity movieActivity;
    public NowPlayingReceiver(MovieActivity activity) {
        this.movieActivity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        if (Objects.equals(action, "now_playing")) {
            movieActivity.showNowPlaying(intent);
        } else if (Objects.equals(action, "resume_slideshow")) {
            movieActivity.startSlideshow();
        }
    }
}