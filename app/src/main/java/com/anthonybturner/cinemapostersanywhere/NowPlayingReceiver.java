package com.anthonybturner.cinemapostersanywhere;
// This class is a BroadcastReceiver that listens for messages from the WebSocket server
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
        String action = intent.getStringExtra("action");
        if (Objects.equals(action, "now_playing")) {
            mainActivity.showNowPlaying(intent);
        } else if (Objects.equals(action, "resume_slideshow")) {
            mainActivity.startSlideshow();
        }
    }
}