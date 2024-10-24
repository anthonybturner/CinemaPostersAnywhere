package com.anthonybturner.cinemapostersanywhere;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.anthonybturner.cinemapostersanywhere.utilities.TileTransformation;
import com.bumptech.glide.Glide;

public class AchievementActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievement);
        initializeViews();
    }

    private void initializeViews() {

        Intent intent = getIntent();
        String name = intent.getStringExtra("achievement_name");
        String status = intent.getStringExtra("achievement_status");
        String iconUrl = intent.getStringExtra("achievement_icon_url");
        String desc = intent.getStringExtra("achievement_des");

        TextView achievemenetNameTextView = findViewById(R.id.achievement_name);
        achievemenetNameTextView.setText(String.format("%s - %s",name, status));

        ImageView achievementIconImageView = findViewById(R.id.achievement_icon_url);
        // Use Glide to load the icon image from URL
        Glide.with(this)
                .load(iconUrl)
                .transform(new TileTransformation(0.2f))  // Apply the tiling transformation
                .placeholder(R.drawable.placeholder_image) // Optional placeholder while loading
                .error(R.drawable.error_image) // Optional error image
                .into(achievementIconImageView);

        TextView achievemenetDescriptionTextView = findViewById(R.id.achievement_description);
        achievemenetDescriptionTextView.setText(desc);
    }

}