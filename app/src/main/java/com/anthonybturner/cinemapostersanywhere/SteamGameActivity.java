package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.MainActivity.CLOSE_NOW_PLAYING_ACTION;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.Models.Video;
import com.anthonybturner.cinemapostersanywhere.Models.YouTubeResponse;
import com.anthonybturner.cinemapostersanywhere.Models.YouTubeVideo;
import com.anthonybturner.cinemapostersanywhere.adapters.VideoAdapter;
import com.anthonybturner.cinemapostersanywhere.interfaces.YouTubeApiService;
import com.bumptech.glide.Glide;
import com.anthonybturner.cinemapostersanywhere.R.menu;

import org.json.JSONArray;
import org.json.JSONException;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.media.AudioManager;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

public class SteamGameActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener{

    private static final String TAG = "SteamGameActivity";
    private static final String YOUTUBE_API_KEY = "AIzaSyBJxsSN8930AS9257v8JwTncSjNDXSlotg"; // Your YouTube API key
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";
    // UI components
    private WebView webView;
    private ProgressBar loadingIndicator;

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private ActionBarDrawerToggle drawerToggle;
    // Video management
    private List<Video> videoList = new ArrayList<>();
    private int currentVideoIndex = 0;
    private int currentFocusPosition;
    private boolean isVideoPaused;

    public static boolean isActive = false;

    private final BroadcastReceiver closeNowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CLOSE_NOW_PLAYING_ACTION.equals(intent.getAction())) {
                finish();
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        isActive = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isVideoPaused = false;
        setContentView(R.layout.activity_steam_game);
        // Initialize UI components and drawer
        initializeUIComponents();
        // Loading indicator for the WebView
        loadingIndicator = findViewById(R.id.loading_indicator);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                closeNowPlayingReceiver,
                new IntentFilter(CLOSE_NOW_PLAYING_ACTION)
        );
        try {
            updateUIWithGameDetails(getIntent());
            String gameName = getIntent().getStringExtra("game_name");
            if (gameName != null) {
                fetchRelatedVideos(gameName); // Fetch related videos based on game name
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static final String VIDEOS_TITLE = "Videos"; // Use exact title from your menu
    public static final String SETTINGS_TITLE = "Settings"; // Use exact title from your menu

    private void initializeUIComponents() {
        webView = findViewById(R.id.youtube_webview);
        setupWebView(videoList);
        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Create video adapter and set it to the RecyclerView
        videoAdapter = new VideoAdapter(videoList, this);
        recyclerView.setAdapter(videoAdapter);
        createDrawer();
    }

    private void createDrawer() {

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);
        // Setup ActionBarDrawerToggle if needed
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onVideoClick(String videoId) {
        //loadVideo(videoId);
        loadVideoUrl(videoId);
    }

    private void fetchRelatedVideos(String gameName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        YouTubeApiService apiService = retrofit.create(YouTubeApiService.class);

        Call<YouTubeResponse> call = apiService.getRelatedVideos(
                gameName,
                YOUTUBE_API_KEY,
                "snippet",
                "video",
                20
        );

        call.enqueue(new Callback<YouTubeResponse>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<YouTubeResponse> call, retrofit2.Response<YouTubeResponse> response) {
                videoList.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (YouTubeVideo video : response.body().getItems()) {
                        String videoId = video.getId().getVideoId();
                        String title = video.getSnippet().getTitle();
                        String thumbnailUrl = video.getSnippet().getThumbnails().getMedium().getUrl();
                        String description = video.getSnippet().getDescription();
                        String channelTitle = video.getSnippet().getChannelTitle();
                        // Create a Video object and add it to the list
                        Video youtubeVideo = new Video(videoId, title, thumbnailUrl, description, channelTitle);
                        videoList.add(youtubeVideo);
                    }
                } else {
                    // Load default video if error
                    videoList.add(new Video("jL_NPJtEeKY", "Apex Coaching, tips and tricks", "https://img.youtube.com/vi/jL_NPJtEeKY/mqdefault.jpg", "Apex Coaching, tips and tricks \uD83D\uDD34 | !merch !merch !merch", "District"));
                    videoList.add(new Video("-bchB88ZCvM", "ONE OF THE CRAZIEST WINS OF MY APEX CAREER | $400,000 BLGS Highlights", "https://img.youtube.com/vi/-bchB88ZCvM/mqdefault.jpg", "ONE OF THE CRAZIEST WINS OF MY APEX CAREER | $400,000 BLGS Highlights", "ItzTimmy"));
                    videoList.add(new Video("XXXzB2KOjtM", "Spectating Ranked Players: Pro vs Casual Positioning – How to Avoid Common Mistakes in Apex Legends\n", "https://img.youtube.com/vi/XXXzB2KOjtM/mqdefault.jpg", "Spectating Ranked Players: Pro vs Casual Positioning – How to Avoid Common Mistakes in Apex Legends\n", "Dazs"));
                    Log.e(TAG, "Response error: " + response.message());
                }
                // Notify adapter about data change to update UI
                videoAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(Call<YouTubeResponse> call, Throwable t) {
                Log.e(TAG, "API call failed: " + t.getMessage());
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(List<Video> videos) {

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        // Enable mixed content mode for Lollipop and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        adjustWebViewSize();
        if (!videos.isEmpty()) {
            loadVideo(videos.get(getNextRandomVideoIndex()).getVideoId()); // Load a random video
        }
    }
    private void adjustWebViewSize() {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) webView.getLayoutParams();
        int width = getResources().getDimensionPixelSize(R.dimen.sidebar_width);
        params.height = (width * 9) / 16; // Maintain 16:9 aspect ratio
        webView.setLayoutParams(params);
    }

    private void loadVideoUrl(String videoId) {
        String videoUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=1&enablejsapi=1";
        webView.loadUrl(videoUrl);
    }
    private void loadVideo(String videoId) {
        // Build the HTML string
        String html = buildHtmlString(videoId);

        // Load the HTML into the WebView
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }
    private String buildHtmlString(String videoId) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>")
                .append("<html>")
                .append("<head>")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<style>")
                .append("body { margin: 0; padding: 0; }")
                .append("iframe { width: 100%; height: 100%; }")
                .append("</style>")
                .append("</head>")
                .append("<body>")
                .append("<iframe id=\"player\" type=\"text/html\" ")
                .append("src=\"https://www.youtube.com/embed/").append(videoId).append("?autoplay=1\" ")
                .append("frameborder=\"0\" allowfullscreen></iframe>")
                .append("</body>")
                .append("</html>");

        return html.toString();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            // Move focus down
            if (currentFocusPosition < videoList.size() - 1) {
                currentFocusPosition++;
              //  videoAdapter.setFocusedPosition(currentFocusPosition);
                return true; // Indicate the key event was handled
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            // Move focus up
            if (currentFocusPosition > 0) {
                currentFocusPosition--;
            //    videoAdapter.setFocusedPosition(currentFocusPosition);
                return true; // Indicate the key event was handled
            }
        }else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (isVideoPaused) {
                webView.evaluateJavascript("javascript:playVideo();", null);    // Play video
            } else {
                webView.evaluateJavascript("javascript:pauseVideo();", null);   // Pause video
            }
            isVideoPaused = !isVideoPaused;
        }return super.onKeyDown(keyCode, event);
    }

    private int getNextRandomVideoIndex() {
        if (videoList.isEmpty()) {
           Log.d(TAG, "No videos available.");
            return -1; // Indicates no video available
        }
        Random random = new Random();
        int randomIndex = random.nextInt(videoList.size());
        return randomIndex;
    }

    private void updateUIWithGameDetails(Intent intent) throws JSONException {
        if (intent != null) {
            String gameName = intent.getStringExtra("game_name");
            String releaseDate = intent.getStringExtra("release_date");
            String description = intent.getStringExtra("description");
            String minimumRequirements = intent.getStringExtra("minimum_requirements");
            String maximumRequirements = intent.getStringExtra("recommended_requirements");
            String posterImageUrl = intent.getStringExtra("poster_image_url");

            SetAchievements(intent);

            TextView gameNameTextView = findViewById(R.id.title);
            TextView gameDescriptionTextView = findViewById(R.id.overview);
            ImageView gamePosterImageView = findViewById(R.id.game_poster);
            TextView minRequirementsTextView = findViewById(R.id.min_requirements);
            TextView maxRequirementsTextView = findViewById(R.id.max_requirements);

            minRequirementsTextView.setFocusable(true);
            minRequirementsTextView.setFocusableInTouchMode(true);
            minRequirementsTextView.setBackgroundResource(R.drawable.achievement_requirements_selector);
            minRequirementsTextView.setOnClickListener(view -> {
                // Handle click event
                Intent minReqIntent = new Intent(this, RequirementsActivity.class);
                minReqIntent.putExtra("requirement_type", "Minimum Requirements");
                minReqIntent.putExtra("description", minimumRequirements);
                minReqIntent.putExtra("poster_image_url", posterImageUrl);
                startActivity(minReqIntent);
                // You can also navigate to another activity or perform other actions here
            });

            maxRequirementsTextView.setFocusable(true);
            maxRequirementsTextView.setFocusableInTouchMode(true);
            maxRequirementsTextView.setBackgroundResource(R.drawable.achievement_requirements_selector);
            maxRequirementsTextView.setOnClickListener(view -> {
                // Handle click event
                Intent recommendedReqIntent = new Intent(this, RequirementsActivity.class);
                recommendedReqIntent.putExtra("requirement_type", "Minimum Requirements");
                recommendedReqIntent.putExtra("description", maximumRequirements);
                recommendedReqIntent.putExtra("poster_image_url", posterImageUrl);
                startActivity(recommendedReqIntent);
                // You can also navigate to another activity or perform other actions here
            });

            gameNameTextView.setText(String.format("Now Playing: %s (%s)", gameName, releaseDate));
            gameDescriptionTextView.setText(description);

            setTextViewMarquee(minRequirementsTextView);
            setTextViewMarquee(maxRequirementsTextView);
            if (posterImageUrl != null) {
                Glide.with(this).load(posterImageUrl).into(gamePosterImageView);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                minRequirementsTextView.setText(Html.fromHtml(minimumRequirements, Html.FROM_HTML_MODE_LEGACY));
                maxRequirementsTextView.setText(Html.fromHtml(maximumRequirements, Html.FROM_HTML_MODE_LEGACY));
            }
        }
    }

    private static void setTextViewMarquee(TextView textView) {
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setSingleLine(true);
        textView.setMarqueeRepeatLimit(-1);  // -1 for infinite marquee
        textView.setHorizontallyScrolling(true);
        textView.setFocusable(true);
        textView.setFocusableInTouchMode(true);
        textView.requestFocus();
        textView.setSelected(true); // Important for marquee
    }

    private void SetAchievements(Intent intent) throws JSONException {
        LinearLayout achievementsLayout = findViewById(R.id.achievement_menu);

        String namesJson = intent.getStringExtra("achievement_names");
        String statusesJson = intent.getStringExtra("achievement_statuses");
        String descJson = intent.getStringExtra("achievement_desc");
        String iconsJson = intent.getStringExtra("achievement_icons");

        JSONArray achievementNames = new JSONArray(namesJson);
        JSONArray achievementStatuses = new JSONArray(statusesJson);
        JSONArray achievementDesc = new JSONArray(descJson);
        JSONArray achievementIcons = new JSONArray(iconsJson);

        // Clear any existing views (optional, if the layout is reused)
        achievementsLayout.removeAllViews();

        // Loop through the arrays and create views for each achievement
        for (int i = 0; i < achievementNames.length(); i++) {

            String name = achievementNames.getString(i);
            String status = achievementStatuses.getString(i);
            String iconUrl = achievementIcons.getString(i);
            String desc =  achievementDesc.getString(i);

            // Create a horizontal LinearLayout to contain icon and texts
            LinearLayout achievementCard = CreateAchievementCardLayout();
            achievementCard.setFocusable(true);
            achievementCard.setFocusableInTouchMode(true);
            achievementCard.setBackgroundResource(R.drawable.achievement_card_selector);
            achievementCard.setPadding(4,4,4,4);

            achievementCard.setOnClickListener(view -> {
                // Handle click event
                intent.setClass(this, AchievementActivity.class);
                intent.putExtra("achievement_name", name);
                intent.putExtra("achievement_status", status);
                intent.putExtra("achievement_icon_url", iconUrl);
                intent.putExtra("achievement_des", desc);
                startActivity(intent);
                // You can also navigate to another activity or perform other actions here
            });

            // Create ImageView for achievement icon
            ImageView iconImageView = CreateAchievementIconView(iconUrl);
            achievementCard.addView(iconImageView);

            // Create TextView for achievement name
            TextView nameTextView = CreateAchievementTextView(name, 6,LinearLayout.LayoutParams.WRAP_CONTENT);
            achievementCard.addView(nameTextView);

            // Create TextView for achievement status (unlocked/locked)
            TextView statusTextView = CreateAchievementTextView(status, 5, LinearLayout.LayoutParams.WRAP_CONTENT);
            //statusTextView.setPadding(20, 10, 20, 10); // Padding inside the TextView
            achievementCard.addView(statusTextView);

            // Create TextView for achievement description (unlocked/locked)
            TextView descTextView =  CreateAchievementTextView(desc, 5, 128);
            descTextView.setMaxLines(1);
            descTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            descTextView.setSingleLine(true);
            descTextView.setMarqueeRepeatLimit(-1);  // -1 for infinite marquee
            descTextView.setHorizontallyScrolling(true);
            descTextView.setFocusable(true);
            descTextView.setFocusableInTouchMode(true);
            descTextView.requestFocus();
            descTextView.setSelected(true); // Important for marquee
            achievementCard.addView(descTextView);
            // Add the row layout to the main achievements layout
            achievementsLayout.addView(achievementCard);
        }
    }
    private @NonNull TextView CreateAchievementTextView(String name, int size, int LayoutParamWidth) {
        TextView achievementTextView = new TextView(this);
        achievementTextView.setText(name);
        achievementTextView.setTextSize(size);
        achievementTextView.setTextColor(getResources().getColor(R.color.white)); // Customize text appearance
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LayoutParamWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        achievementTextView.setLayoutParams(layoutParams);
        return achievementTextView;
    }

    private @NonNull ImageView CreateAchievementIconView(String iconUrl) {
        ImageView iconImageView = new ImageView(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                80, LinearLayout.LayoutParams.MATCH_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        iconImageView.setLayoutParams(layoutParams);
        iconImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE); // Set scale type

        // Use Glide to load the icon image from URL
        Glide.with(this)
                .load(iconUrl)
                .placeholder(R.drawable.placeholder_image) // Optional placeholder while loading
                .error(R.drawable.error_image) // Optional error image
                .into(iconImageView);
        return iconImageView;
    }

    private @NonNull LinearLayout CreateAchievementCardLayout() {
        LinearLayout achievementCard = new LinearLayout(this);
        achievementCard.setOrientation(LinearLayout.VERTICAL);
        achievementCard.setPadding(8, 8, 8, 8); // Add padding between achievements

        // Create LayoutParams and set the margins
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                128 ,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linearLayoutParams.setMargins(2, 2, 2, 2); // Left, Top, Right, Bottom margins in pixels
        achievementCard.setLayoutParams(linearLayoutParams);
        return achievementCard;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeNowPlayingReceiver);
    }
}
