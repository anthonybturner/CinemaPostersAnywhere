package com.anthonybturner.cinemapostersanywhere;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;

import java.util.Objects;

public class NowPlayingActivity extends AppCompatActivity {

    private final BroadcastReceiver closeNowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), MainActivity.CLOSE_NOW_PLAYING_ACTION)) {
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now_playing);
        Intent intent = getIntent();
        setupUI(intent);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                closeNowPlayingReceiver,
                new IntentFilter(MainActivity.CLOSE_NOW_PLAYING_ACTION)
        );
    }

    private void setupUI(Intent intent) {
        setTextViewContent(R.id.movie_title, getFormattedTitle(intent));
        setTextViewContent(R.id.movie_overview, intent.getStringExtra("overview"));
        setTextViewContent(R.id.movie_studio, intent.getStringExtra("studio"));
        setTextViewContent(R.id.movie_ratings, intent.getStringExtra("contentRating"));
        loadImageIntoView(intent.getStringExtra("posterUrl"), findViewById(R.id.movie_poster));
        setupActorRoles(intent.getBundleExtra("actorBundle"));
    }

    private String getFormattedTitle(Intent intent) {
        String title = intent.getStringExtra("title");
        String year = intent.getStringExtra("year");
        return (year != null && !year.isEmpty()) ? String.format("%s (%s)", title, year) : title;
    }

    private void setTextViewContent(int textViewId, String content) {
        TextView textView = findViewById(textViewId);
        if (textView != null) {
            textView.setText(content);
        }
    }
    private void setupActorRoles(Bundle rolesBundle) {
        if (rolesBundle == null) return;

        LinearLayout sideMenuLayout = findViewById(R.id.side_button_menu);
        sideMenuLayout.removeAllViews();

        for (String key : rolesBundle.keySet()) {
            if (key.endsWith("_role")) {
                createRoleView(key, rolesBundle, sideMenuLayout);
            }
        }
    }

    private void createRoleView(String key, Bundle rolesBundle, LinearLayout parentLayout) {
        String actorName = key.replace("_role", "");
        String actorRole = rolesBundle.getString(key);
        String actorImageUrl = rolesBundle.getString(actorName + "_thumb");

        CardView cardView = createCardView();
        LinearLayout horizontalLayout = createLinearLayout(LinearLayout.HORIZONTAL, 0, 12, 0 , 0);
        cardView.addView(horizontalLayout);

        ImageView imageView = loadActorImage();
        loadImageIntoView(actorImageUrl, imageView);
        horizontalLayout.addView(imageView);

        LinearLayout textLayout = createInnerTextLayout();
        horizontalLayout.addView(textLayout);
        textLayout.addView(createTextView(String.format("%s as %s",actorName,actorRole), android.R.color.white));
        parentLayout.addView(cardView);
    }

    private CardView createCardView() {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //int marginPx = (int) (1 * getResources().getDisplayMetrics().density);
        //params.setMargins(marginPx, marginPx, marginPx, marginPx);
        cardView.setLayoutParams(params);
        cardView.setCardElevation(4f);
        cardView.setRadius(8f);
        cardView.setBackgroundResource(R.drawable.transparent_background);
        return cardView;
    }

    private LinearLayout createLinearLayout(int orientation, int leftPadding, int topPadding, int rightPadding, int bottomPadding) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(orientation);
        layout.setPadding(leftPadding, topPadding, rightPadding, bottomPadding); // Set the padding
        return layout;
    }

    private LinearLayout createInnerTextLayout() {
        LinearLayout layout = createLinearLayout(LinearLayout.VERTICAL, 0, 0 , 0 , 0);
        layout.setGravity(Gravity.CENTER);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return layout;
    }

    private TextView createTextView(String text, int colorRes) {
        TextView textView = new TextView(this);
        textView.setGravity(Gravity.CENTER);
        textView.setText(text);
        textView.setTextSize(8);
        textView.setTextColor(getResources().getColor(colorRes));
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        return textView;
    }

    private void loadImageIntoView(String url, ImageView imageView) {
        Glide.with(this)
                .load(url != null && !url.isEmpty() ? url : R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .placeholder(R.drawable.placeholder_image)
                .into(imageView);
    }

    private ImageView loadActorImage() {
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(72, 72);
        params.setMargins(8, 4, 2, 2);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return imageView;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeNowPlayingReceiver);
    }
}
