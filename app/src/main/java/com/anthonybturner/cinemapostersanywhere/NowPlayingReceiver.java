package com.anthonybturner.cinemapostersanywhere;
// This class is a BroadcastReceiver that listens for messages from the WebSocket server
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants;

import java.util.Objects;

public class NowPlayingReceiver extends BroadcastReceiver {
    private final MainActivity mainActivity;
    public NowPlayingReceiver(MainActivity activity) {
        this.mainActivity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        if (Objects.equals(action, MovieConstants.ACTION_MOVIE_NOW_PLAYING)) {
            mainActivity.showNowPlaying(intent);
        } else if (Objects.equals(action, MovieConstants.ACTION_MOVIE_RESUME_SLIDESHOW)) {
            mainActivity.stopSlideshow();
            mainActivity.startSlideshow();
        }
    }
}