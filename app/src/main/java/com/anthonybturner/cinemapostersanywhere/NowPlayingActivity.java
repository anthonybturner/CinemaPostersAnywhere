package com.anthonybturner.cinemapostersanywhere;

import static androidx.core.util.TypedValueCompat.dpToPx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.Image;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
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
        // Get the movie data passed from MainActivity (use Intent)
        Intent intent = getIntent();
        createMovieTitle(intent);
        createMovieOverview(intent);
        createMovieStudio(intent);
        createContentRating(intent);
        createActorRoles(intent);
        createMoviePoster(intent);
        // Update UI
        LocalBroadcastManager.getInstance(this).registerReceiver(closeNowPlayingReceiver,
                new IntentFilter("com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING"));
    }

    private void createMovieStudio(Intent intent) {
        String studio = intent.getStringExtra("studio");
        TextView movieStudioTextView = findViewById(R.id.movie_studio);
        if(movieStudioTextView != null)
            movieStudioTextView.setText(studio);
    }
    private void createContentRating(Intent intent) {
        String rating = intent.getStringExtra("contentRating");
        TextView movieRatingTextView = findViewById(R.id.movie_ratings);
        if(movieRatingTextView != null)
            movieRatingTextView.setText(rating);
    }

    private void createMoviePoster(Intent intent) {
        String posterUrl = intent.getStringExtra("posterUrl");
        ImageView posterImageView = findViewById(R.id.movie_poster);
        if (posterImageView != null)
            Glide.with(this).load(posterUrl).into(posterImageView);
    }
    private void createMovieOverview(Intent intent) {
        String overview = intent.getStringExtra("overview");
        TextView movieOverviewTextView = findViewById(R.id.movie_overview);
        if(movieOverviewTextView != null)
            movieOverviewTextView.setText(overview);
    }
    private void createMovieTitle(Intent intent) {
        String title = intent.getStringExtra("title");
        String year = intent.getStringExtra(("year"));
        if (!Objects.equals(year, "")){
            title = String.format("%s (%s)", title, year);
        }
        TextView movieTitleTextView = findViewById(R.id.movie_title);
        if(movieTitleTextView != null)
            movieTitleTextView.setText(title);
    }
    private void createActorRoles(Intent intent) {
        Bundle rolesBundle = intent.getBundleExtra("actorBundle");
        //Bundle rolesNotMainBundle = intent.getBundleExtra("actorNotMainRoleBundle");
        if (rolesBundle == null ) return;
        // Get the LinearLayout from your layout
        LinearLayout linearLayout = findViewById(R.id.side_button_menu);
        linearLayout.removeAllViews();
        for (String key : rolesBundle.keySet()) {
            if (key.endsWith("_role")) {
                createRole(key, rolesBundle);
            }
        }
    }
    private void createRole(String key, Bundle rolesBundle) {
        String actorName = key.replace("_role", ""); // Get the actor's name
        String actorRole = rolesBundle.getString(key); // Get the actor's role
        String actorImageUrl = rolesBundle.getString(actorName + "_thumb"); // Get the corresponding thumb URL
        // Create a new CardView to hold the actor details
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(4f);
        cardView.setRadius(8f);
        cardView.setBackgroundResource(R.drawable.transparent_background);
        // Set margins (left, top, right, bottom)
        int marginInDp = 4; // Set desired margin in dp
        int marginInPx = (int) (marginInDp * getResources().getDisplayMetrics().density); // Convert dp to pixels
        cardParams.setMargins(marginInPx, marginInPx, marginInPx, marginInPx);

        // Create a LinearLayout for horizontal stacking of ImageView and TextView container
        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL); // Set orientation to horizontal
        cardView.addView(innerLayout); // Add the inner LinearLayout to the CardView

        // Create and set up the ImageView for the actor's image
        ImageView imageView = loadActorImage(); // Create ImageView using the helper method
        // Load the image into the ImageView using Glide
        if (actorImageUrl != null && !actorImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(actorImageUrl)
                    .error(R.drawable.error_image) // Load this if there's an error
                    .placeholder(R.drawable.placeholder_image) // Load this while the image is loading
                    .into(imageView);
        } else {
            // If the actor image URL is empty or null, load a default image
            Glide.with(this)
                    .load(R.drawable.placeholder_image) // Load your default image here
                    .into(imageView);
        }
        innerLayout.addView(imageView); // Add the ImageView to the inner LinearLayout

        // Create a LinearLayout for vertical stacking of the actor's name and role
        LinearLayout innerTextLayout = new LinearLayout(this);
        //innerTextLayout.setBackgroundResource(R.drawable.transparent_background_white);
        innerTextLayout.setGravity(Gravity.CENTER);

        innerTextLayout.setOrientation(LinearLayout.VERTICAL); // Set orientation to vertical
        LinearLayout.LayoutParams textLayoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width will be weighted (fill remaining space)
                ViewGroup.LayoutParams.MATCH_PARENT); // Height
        textLayoutParams.weight = 1; // Give this layout remaining space
        innerTextLayout.setLayoutParams(textLayoutParams);
        innerLayout.addView(innerTextLayout); // Add the vertical layout to the horizontal layout

        // Create and set up the TextView for the actor's name
        TextView actorNameTextView = new TextView(this);
        actorNameTextView.setText(actorName); // Set the actor's name
        actorNameTextView.setTextSize(8);
        //actorNameTextView.setTextAppearance(this, android.R.style.TextAppearance_Holo_Small); // Change to Medium for better visibility
        actorNameTextView.setTextColor(getResources().getColor(android.R.color.white)); // Set text color
        // Set layout parameters to center the text
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Width wraps content
                ViewGroup.LayoutParams.WRAP_CONTENT); // Height wraps content
        textParams.gravity = Gravity.CENTER; // Center the text
        actorNameTextView.setLayoutParams(textParams); // Apply layout parameters

        innerTextLayout.addView(actorNameTextView); // Add the TextView to the vertical layout

        // Create and set up the TextView for the character's role
        TextView characterRoleTextView = new TextView(this);
        characterRoleTextView.setText(actorRole); // Set the character role
        characterRoleTextView.setTextSize(8);
        //characterRoleTextView.setTextAppearance(this, android.R.style.TextAppearance_Holo_Small); // Change to Medium for better visibility
        characterRoleTextView.setTextColor(getResources().getColor(android.R.color.darker_gray)); // Use a slightly lighter color for contrast
        characterRoleTextView.setLayoutParams(textParams); // Apply layout parameters

        innerTextLayout.addView(characterRoleTextView); // Add the TextView to the vertical layout

        // Finally, add the CardView to the main LinearLayout
        LinearLayout mainLayout = findViewById(R.id.side_button_menu); // Your main layout
        mainLayout.addView(cardView);
    }

    private ImageView loadActorImage() {
        // Create an ImageView and load the image using Glide or Picasso
        ImageView imageView = new ImageView(this);
        // Set layout parameters (example)
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                80, // Fixed width in dp, change to appropriate value
                80 // Fixed height in dp, adjust based on your preference
        );
        layoutParams.setMargins(8, 4, 8, 4); // Set margins for spacing
        imageView.setLayoutParams(layoutParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Scale type for better image fitting
        return imageView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeNowPlayingReceiver);
    }
}