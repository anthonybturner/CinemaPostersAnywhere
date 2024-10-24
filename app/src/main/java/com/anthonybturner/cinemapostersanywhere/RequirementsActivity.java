package com.anthonybturner.cinemapostersanywhere;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class RequirementsActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requirements);
        initializeViews();
    }

    private void initializeViews() {
        Intent intent = getIntent();
        String description = intent.getStringExtra("description");
        String imageUrl = intent.getStringExtra("poster_image_url");

        TextView descrtionTextView = findViewById(R.id.requirement_description);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            descrtionTextView.setText(Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY));
        }
        ImageView posterImageView = findViewById(R.id.poster_image);
        // Use Glide to load the icon image from URL
        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image) // Optional placeholder while loading
                .error(R.drawable.error_image) // Optional error image
                .into(posterImageView);
    }
}