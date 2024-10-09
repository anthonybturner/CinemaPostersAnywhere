package com.anthonybturner.cinemapostersanywhere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Objects;

public class NowPlayingReceiver extends BroadcastReceiver {
    private final MainActivity mainActivity;
    
    public NowPlayingReceiver(MainActivity activity) {
        this.mainActivity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String overview = intent.getStringExtra("overview");
        String year = intent.getStringExtra("year");
        String posterUrl = intent.getStringExtra("posterUrl");
        String action = intent.getStringExtra("action");

        if (Objects.equals(action, "now_playing")) {
            mainActivity.showNowPlaying(title, year, posterUrl, overview);
        } else if (Objects.equals(action, "resume_slideshow")) {
            mainActivity.startSlideshow();
        }
    }
}