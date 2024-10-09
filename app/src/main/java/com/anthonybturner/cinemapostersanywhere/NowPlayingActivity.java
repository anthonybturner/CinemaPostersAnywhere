package com.anthonybturner.cinemapostersanywhere;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;

import java.util.Objects;

public class NowPlayingActivity extends AppCompatActivity {

    private final BroadcastReceiver closeNowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING")) {
                finish();  // Close the NowPlayingActivity
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        // Find views by ID
        TextView movieTitleTextView = findViewById(R.id.movie_title);
        TextView movieOverviewTextView = findViewById(R.id.movie_overview);
        ImageView posterImageView = findViewById(R.id.movie_poster);
        // Get the movie data passed from MainActivity (use Intent)
        String title = getIntent().getStringExtra("title");
        String overview = getIntent().getStringExtra("overview");
        String posterUrl = getIntent().getStringExtra("poster_url");
        // Update UI
        movieTitleTextView.setText(title);
        movieOverviewTextView.setText(overview);
        Glide.with(this).load(posterUrl).into(posterImageView); // Register the receiver to listen for close broadcasts
        LocalBroadcastManager.getInstance(this).registerReceiver(closeNowPlayingReceiver,
                new IntentFilter("com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING"));
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeNowPlayingReceiver);
    }
}