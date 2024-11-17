package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.utilities.MovieConstants.CLOSE_NOW_PLAYING_ACTION;
import static com.anthonybturner.cinemapostersanywhere.utilities.ApexLegendsAPIConstants.APEX_LEGENDS_API_UPDATE_ACTION;
import static com.anthonybturner.cinemapostersanywhere.utilities.Converters.CalculateTime;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.Models.Video;
import com.anthonybturner.cinemapostersanywhere.Models.YouTubeResponse;
import com.anthonybturner.cinemapostersanywhere.Models.YouTubeVideo;
import com.anthonybturner.cinemapostersanywhere.adapters.VideoAdapter;
import com.anthonybturner.cinemapostersanywhere.interfaces.TimerUpdateListener;
import com.anthonybturner.cinemapostersanywhere.interfaces.YouTubeApiService;
import com.anthonybturner.cinemapostersanywhere.services.ApexLegendsAPIService;
import com.anthonybturner.cinemapostersanywhere.services.SteamGameService;
import com.anthonybturner.cinemapostersanywhere.utilities.Converters;
import com.anthonybturner.cinemapostersanywhere.utilities.MovieConstants;
import com.anthonybturner.cinemapostersanywhere.utilities.SteamConstants;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import androidx.appcompat.app.ActionBarDrawerToggle;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.recyclerview.widget.RecyclerView;

public class SteamGameActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener, TimerUpdateListener {
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";

    // UI Components
    private WebView webView;
    private RecyclerView recyclerView;
    private VideoAdapter videoAdapter;
    private ActionBarDrawerToggle drawerToggle;
    private TextView pubMapDurationTextView;
    private TextView rankedMapDurationTextView;
    private TextView arenasMapDurationTextView;

    // Video Management
    private final List<Video> videoList = new ArrayList<>();
    private final int currentVideoIndex = 0;
    private int currentFocusPosition;
    private boolean isVideoPaused;

    // Service Management
    public static boolean isActive = false;
    private SteamGameService steamGameService;
    private ApexLegendsAPIService apexLegendsService;
    private boolean bound = false;
    private boolean apex_bound = false;
    // Broadcast Receivers
    private final BroadcastReceiver steamUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String steamData = intent.getStringExtra("steamData");
            Log.d(SteamConstants.TAG, "Received Steam update: " + steamData);
            // Update the UI with the received data
            if (steamData != null) {
                //steamDataTextView.setText(steamData);
            }
        }
    };

    private final BroadcastReceiver closeNowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (CLOSE_NOW_PLAYING_ACTION.equals(intent.getAction())) {
                finish(); // Close the activity
            }
        }
    };

    // Service Connection
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // Handle connection established with the service
            if (name.getClassName().equals(SteamGameService.class.getName())) {
                SteamGameService.LocalBinder binder = (SteamGameService.LocalBinder) service;
                steamGameService = binder.getService();
                bound = true;
            } else if (name.getClassName().equals(ApexLegendsAPIService.class.getName())) {
                ApexLegendsAPIService.LocalBinder apexBinder = (ApexLegendsAPIService.LocalBinder) service;
                apexLegendsService = apexBinder.getService();
                apexLegendsService.setTimerCallback(SteamGameActivity.this);
                apex_bound = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
            apex_bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isVideoPaused = false;
        setContentView(R.layout.activity_steam_game);
        initializeUIComponents();
        setupWebView(videoList);
        createDrawer();
        Intent intent = getIntent();
        registerReceivers(intent);
        updateUIFromIntent(intent);
    }

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
    public void onTimerStarted(Intent intent, boolean isRanked, boolean isArenas) {

        if(intent.hasExtra("battle_royale_map")){//pull game info for public and ranked
            if(isRanked){
                createRankedMapInfo(intent);
            }else if(isArenas){
                createArenaMapInfo(intent);
            }else{
                createPublicMapInfo(intent);
            }
            String posterImageUrl = intent.getStringExtra("battle_royale_asset");//If a map, use the map's image for the background of the activity
            if (posterImageUrl != null) {
                ImageView gamePosterImageView = findViewById(R.id.game_poster);
                Glide.with(this).load(posterImageUrl).into(gamePosterImageView);
            }
            //clearGameMapInfo();
        }
    }

    @Override
    public void onPlayerStatsTimerStarted(Intent intent, boolean isRanked) {
        if(isRanked){
            createPlayerRankedStats(intent);
        }
    }

    private void createPlayerRankedStats(Intent intent) {

        String name = intent.getStringExtra("name");
        String level = intent.getStringExtra("level");
        String tag = intent.getStringExtra("tag");
        String avatar = intent.getStringExtra("avatar");
        String platform = intent.getStringExtra("platform");
        String toNextLevelPercent = intent.getStringExtra("toNextLevelPercent");

        String rankScore = intent.getStringExtra("rankScore");
        String rankName = intent.getStringExtra("rankName");
        String rankDiv = intent.getStringExtra("rankDiv");
        String rankImg = intent.getStringExtra("rankImg");

        TextView nameTextView = findViewById(R.id.name);
        nameTextView.setText(String.format("%s", name));

        TextView rankNameTextView = findViewById(R.id.rankName);
        rankNameTextView.setText(String.format("%s", rankName));

        TextView rankLevelTextView = findViewById(R.id.level);
        rankLevelTextView.setText(String.format("Level: %s", level));

        ImageView rankImgImageView = findViewById(R.id.rankImg);
        if (rankImg != null) {
            Glide.with(this).load(rankImg).into(rankImgImageView);
        }
        ImageView avatarImageView = findViewById(R.id.avatar);
        if (avatar != null) {
            Glide.with(this).load(avatar).into(avatarImageView);
        }
    }

    @Override
    public void onTimerUpdate(long durationInMillis,boolean isRanked, boolean isArenas) {
        if(isRanked) {
            updateRankedCountdownTextView(durationInMillis);
        }else if(isArenas) {
            updateArenasCountdownTextView(durationInMillis);
        }else if( !isRanked && !isArenas) {
            updatePublicCountdownTextView(durationInMillis);
        }
    }

    @Override
    public void onTimerFinish(boolean isRanked){

    }


    private void updateUIFromIntent(Intent intent) {
        try {
            updateUIWithGameDetails(intent);
            String gameName = intent.getStringExtra("game_name");
            if (gameName != null) {
                fetchRelatedVideos(gameName); // Fetch related videos based on game name
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void registerReceivers(Intent intent) {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                closeNowPlayingReceiver,
                new IntentFilter(CLOSE_NOW_PLAYING_ACTION)
        );
        LocalBroadcastManager.getInstance(this).registerReceiver(
                steamUpdateReceiver,
                new IntentFilter(APEX_LEGENDS_API_UPDATE_ACTION)
        );
        Intent serviceIntent = new Intent(this, SteamGameService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        boolean  isPlayingApexLegends = intent.getBooleanExtra("isPlayingApexLegends", false);
        if(isPlayingApexLegends){
            Intent apexLegendsIntent = new Intent(this, ApexLegendsAPIService.class);
            bindService(apexLegendsIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void initializeUIComponents() {
        videoAdapter = new VideoAdapter(videoList, this);
        webView = findViewById(R.id.youtube_webview);
        pubMapDurationTextView = findViewById(R.id.public_map_duration);
        rankedMapDurationTextView = findViewById(R.id.ranked_map_duration);
        arenasMapDurationTextView = findViewById(R.id.arenas_map_duration);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int currentPosition = videoAdapter.focusedPosition;
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    if (currentPosition < videoAdapter.getItemCount() - 1) {
                        videoAdapter.setFocusedPosition(currentPosition + 1);
                        recyclerView.scrollToPosition(currentPosition + 1);
                        return true; // Indicate that the event was handled
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (currentPosition > 0) {
                        videoAdapter.setFocusedPosition(currentPosition - 1);
                        recyclerView.scrollToPosition(currentPosition - 1);
                        return true; // Indicate that the event was handled
                    }
                }
            }
            return false; // Let other listeners handle it
        });
        recyclerView.setAdapter(videoAdapter);
    }

    private void createDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Initialize DrawerLayout
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
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
        loadVideo(videoId);
    }
    private void fetchRelatedVideos(String gameName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        YouTubeApiService apiService = retrofit.create(YouTubeApiService.class);
        Call<YouTubeResponse> call = apiService.getRelatedVideos(
                gameName,
                MovieConstants.YOUTUBE_API_KEY,
                "snippet",
                "video",
                10
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
                    Log.e(SteamConstants.TAG, "Response error: " + response.message());
                }
                // Notify adapter about data change to update UI
                videoAdapter.notifyDataSetChanged();
                if (!videoList.isEmpty()) {
                    loadVideo(videoList.get(getNextRandomVideoIndex()).getVideoId()); // Load a random video
                }
            }
            @Override
            public void onFailure(Call<YouTubeResponse> call, Throwable t) {
                Log.e(SteamConstants.TAG, "API call failed: " + t.getMessage());
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(List<Video> videos) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.isFocusable();
        webView.isFocusableInTouchMode();
        webView.requestFocus();
        // Enable mixed content mode for Lollipop and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.setWebViewClient(new WebViewClient() {});

     // adjustWebViewSize();
    }
    private void adjustWebViewSize() {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) webView.getLayoutParams();
        int width = getResources().getDimensionPixelSize(R.dimen.sidebar_width);
        params.height = (width * 9) / 16; // Maintain 16:9 aspect ratio
        //params.height = 500;
         webView.setLayoutParams(params);
    }
    private void loadVideo(String videoId) {
        String videoUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=0&enablejsapi=1&mute=0";
        String htmlContent = "<html><body style='margin:0;padding:0;'><iframe width='100%' height='100%' src='" + videoUrl + "' frameborder='0' allow='autoplay; encrypted-media' allowfullscreen></iframe></body></html>";
        webView.loadData(htmlContent, "text/html", "UTF-8");
    }
    private String loadHtmlFromAssets(String fileName) {
        StringBuilder html = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open(fileName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html.toString();
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
       if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
            if (isVideoPaused) {
                webView.evaluateJavascript("(function() { if (player.getPlayerState() === 1) { pauseVideo(); } else { playVideo(); } })();", null);
                // Play video
            } else {
                webView.evaluateJavascript("javascript:pauseVideo();", null);   // Pause video
            }
            isVideoPaused = !isVideoPaused;
           return true;
       }
       if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            // Evaluate JavaScript to play/pause the video
            webView.evaluateJavascript(
                    "(function() { if (player.getPlayerState() === 1) { pauseVideo(); } else { playVideo(); } })();",
                    null
            );
            return true; // Indicate that the event has been handled
        }
        return super.onKeyDown(keyCode, event);
    }

    private int getNextRandomVideoIndex() {
        if (videoList.isEmpty()) {
           Log.d(SteamConstants.TAG, "No videos available.");
            return -1; // Indicates no video available
        }
        Random random = new Random();
        int randomIndex = random.nextInt(videoList.size());
        return randomIndex;
    }
    private void updateUIWithGameDetails(Intent intent) throws JSONException {
        if (intent != null) {
            TextView gameNameTextView = findViewById(R.id.title);
            TextView gameDescriptionTextView = findViewById(R.id.overview);
            ImageView gamePosterImageView = findViewById(R.id.game_poster);
            TextView steamNameTextView = findViewById(R.id.steam_name);


            String releaseDate = intent.getStringExtra("release_date");
            String description = intent.getStringExtra("description");

            String personaName = intent.getStringExtra("personaname");
            steamNameTextView.setText(personaName);

            ImageView avatarImageView = findViewById(R.id.steam_avatar);
            String avatarUrl = intent.getStringExtra("avatarfull");
            if (avatarUrl != null) {
                Glide.with(this).load(avatarUrl).into(avatarImageView);
            }

            //String realName = intent.getStringExtra("realname");
            String posterImageUrl =  "";

            if(intent.hasExtra("achievement_names")) {
                CreateAchievements(intent);
            }

            posterImageUrl = intent.getStringExtra("poster_image_url");//Otherwise, use the steam game poster for the background
            String minimumRequirements = intent.getStringExtra("minimum_requirements");
            createMinRequirements(minimumRequirements, posterImageUrl);

            String maximumRequirements = intent.getStringExtra("recommended_requirements");
            createMaxRequirements(maximumRequirements, posterImageUrl);

            String gameName = intent.getStringExtra("game_name");
            gameNameTextView.setText(String.format("%s (%s)", gameName, releaseDate));

            gameDescriptionTextView.setText(description);
            if (posterImageUrl != null) {
                Glide.with(this).load(posterImageUrl).into(gamePosterImageView);
            }
        }
    }
    private void clearGameMapInfo() {
        clearPublicGameMap();
        clearPublicNextMap();
        clearRankedGameMap();
        clearRankedNextMap();
        clearArenasGameMap();
        clearArenasNextMap();
    }
    private void clearPublicGameMap() {
        TextView gameModeTextView = findViewById(R.id.public_game_mode);
        gameModeTextView.setText("");

        TextView durationTextView = findViewById(R.id.public_map_duration);
        durationTextView.setText("");

        TextView publicMapTextView = findViewById(R.id.public_map);
        publicMapTextView.setText("");

    }
    private void clearRankedGameMap() {
        TextView gameModeTextView = findViewById(R.id.ranked_game_mode);
        gameModeTextView.setText("");

        TextView durationTextView = findViewById(R.id.ranked_map_duration);
        durationTextView.setText("");

        TextView rankedMapTextView = findViewById(R.id.ranked_map);
        rankedMapTextView.setText("");
    }
    private void clearArenasGameMap() {
    }
    private void clearPublicNextMap() {
        TextView nextMapTextView = findViewById(R.id.next_public_map);
        nextMapTextView.setText("");

        TextView nextMapDuractionTextView = findViewById(R.id.next_public_map_duration);
        nextMapDuractionTextView.setText("");

        TextView nextMapStartDateTextView = findViewById(R.id.next_public_map_start_date);
        nextMapStartDateTextView.setText("");

        TextView nextMapEndDateTextView = findViewById(R.id.next_public_map_end_date);
        nextMapEndDateTextView.setText("");
    }

    private void clearRankedNextMap(){
        TextView nextRankedMapTextView = findViewById(R.id.next_ranked_map);
        nextRankedMapTextView.setText("");

        TextView mapDuractionTextView = findViewById(R.id.next_ranked_map_duration);
        mapDuractionTextView.setText("");

        TextView startDateTextView = findViewById(R.id.next_ranked_map_start_date);
        startDateTextView.setText("");

        TextView mapEndDateTextView = findViewById(R.id.next_ranked_map_end_date);
        mapEndDateTextView.setText("");
    }
    private void clearArenasNextMap() {
        TextView nextArenasMapTextView = findViewById(R.id.next_arenas_map);
        nextArenasMapTextView.setText("");

        TextView mapDuractionTextView = findViewById(R.id.next_arenas_map_duration);
        mapDuractionTextView.setText("");

        TextView startDateTextView = findViewById(R.id.next_arenas_map_start_date);
        startDateTextView.setText("");

        TextView mapEndDateTextView = findViewById(R.id.next_arenas_map_end_date);
        mapEndDateTextView.setText("");
    }

    private void createPublicMapInfo(Intent intent) {
        createPublicGameMap(intent);
        createPublicNextMap(intent);
    }
    private void createRankedMapInfo(Intent intent) {
        createRankedGameMap(intent);
        createRankedNextMap(intent);
    }
    private void createArenaMapInfo(Intent intent) {

        createArenasGameMap(intent);
        createArenasNextMap(intent);
    }
    private void hideGameMapInfo() {
        findViewById(R.id.public_map_layout).setVisibility(View.GONE);
        findViewById(R.id.ranked_map_layout).setVisibility(View.GONE);
    }

    private void createPublicGameMap(Intent intent) {
        TextView currentMapTextView = findViewById(R.id.public_map);
        currentMapTextView.setText(String.format("Map: %s", intent.getStringExtra("battle_royale_map")));
        currentMapTextView.setVisibility(View.VISIBLE);
        findViewById(R.id.public_map_layout).setVisibility(View.VISIBLE);
    }
    private void createRankedGameMap(Intent intent) {
        TextView currentMapTextView = findViewById(R.id.ranked_map);
        currentMapTextView.setText(String.format("Map: %s", intent.getStringExtra("ranked_map")));
        findViewById(R.id.ranked_map_layout).setVisibility(View.VISIBLE);
    }
    private void createArenasGameMap(Intent intent) {
        TextView currentMapTextView = findViewById(R.id.arenas_map);
        currentMapTextView.setText(String.format("Map: %s", intent.getStringExtra("arenas_map")));
        findViewById(R.id.arenas_map_layout).setVisibility(View.VISIBLE);

        ConstraintLayout mapLayout = findViewById(R.id.arenas_map_layout);
        String imageUrl =  intent.getStringExtra("arenas_asset");
    }
    private void createPublicNextMap(Intent intent) {
        TextView nextMapTextView = findViewById(R.id.next_public_map);
        nextMapTextView.setText(String.format("Next Map: %s", intent.getStringExtra("next_battle_royale_map")));
        TextView nextMapDuractionTextView = findViewById(R.id.next_public_map_duration);
        nextMapDuractionTextView.setText(String.format("Duration: %s Minutes", intent.getStringExtra("battle_royale_next_DurationInMinutes")));

        TextView nextMapStartDateTextView = findViewById(R.id.next_public_map_start_date);
        String nextReadableDateStart = intent.getStringExtra("next_battle_royale_readableDate_start");
        nextReadableDateStart = Converters.convertToFriendlyDate(nextReadableDateStart);
        nextMapStartDateTextView.setText(String.format("Start Date: %s", nextReadableDateStart));

        TextView nextMapEndDateTextView = findViewById(R.id.next_public_map_end_date);
        String nextReadableDateEnd = intent.getStringExtra("battle_royale_next_readableDate_end");
        nextReadableDateEnd = Converters.convertToFriendlyDate(nextReadableDateEnd);
        nextMapEndDateTextView.setText(String.format("End Date: %s",nextReadableDateEnd ));

    }
    private void createRankedNextMap(Intent intent) {
        TextView nextRankedMapTextView = findViewById(R.id.next_ranked_map);
        nextRankedMapTextView.setText(String.format("Next Map: %s", intent.getStringExtra("next_ranked_map")));

        TextView mapDuractionTextView = findViewById(R.id.next_ranked_map_duration);
        mapDuractionTextView.setText(String.format("Duration: %s Minutes", intent.getStringExtra("next_ranked_DurationInMinutes")));

        TextView startDateTextView = findViewById(R.id.next_ranked_map_start_date);
        String nextReadableDateStart = intent.getStringExtra("next_ranked_readableDate_start");
        nextReadableDateStart = Converters.convertToFriendlyDate(nextReadableDateStart);
        startDateTextView.setText(String.format("Start Date: %s", nextReadableDateStart));

        TextView mapEndDateTextView = findViewById(R.id.next_ranked_map_end_date);
        String nextReadableDateEnd = intent.getStringExtra("next_ranked_readableDate_end");
        nextReadableDateEnd = Converters.convertToFriendlyDate(nextReadableDateEnd);
        mapEndDateTextView.setText(String.format("End Date: %s",nextReadableDateEnd ));
    }
    private void createArenasNextMap(Intent intent) {
        TextView nextArenasMapTextView = findViewById(R.id.next_arenas_map);
        nextArenasMapTextView.setText(String.format("Next Map: %s", intent.getStringExtra("next_arenas_map")));

        TextView mapDuractionTextView = findViewById(R.id.next_arenas_map_duration);
        mapDuractionTextView.setText(String.format("Duration: %s Minutes", intent.getStringExtra("next_arenas_DurationInMinutes")));

        TextView startDateTextView = findViewById(R.id.next_arenas_map_start_date);
        String nextReadableDateStart = intent.getStringExtra("next_arenas_readableDate_start");
        nextReadableDateStart = Converters.convertToFriendlyDate(nextReadableDateStart);
        startDateTextView.setText(String.format("Start Date: %s", nextReadableDateStart));

        TextView mapEndDateTextView = findViewById(R.id.next_arenas_map_end_date);
        String nextReadableDateEnd = intent.getStringExtra("next_arenas_readableDate_end");
        nextReadableDateEnd = Converters.convertToFriendlyDate(nextReadableDateEnd);
        mapEndDateTextView.setText(String.format("End Date: %s",nextReadableDateEnd ));
    }

    private void updatePublicCountdownTextView(long millisUntilFinished) {
        String time = CalculateTime(millisUntilFinished);
        pubMapDurationTextView.setText(time);
    }
    private void updateRankedCountdownTextView(long millisUntilFinished) {
        String time = CalculateTime(millisUntilFinished);
        rankedMapDurationTextView.setText(time);
    }
    private void updateArenasCountdownTextView(long millisUntilFinished) {
        String time = CalculateTime(millisUntilFinished);
        arenasMapDurationTextView.setText(time);
    }
    private void createMinRequirements(String minimumRequirements, String posterImage) {
        TextView minRequirementsTextView = findViewById(R.id.min_requirements);
        if(Objects.equals(minimumRequirements, "")) {
            minRequirementsTextView.setVisibility(View.GONE);
            return;
        }
        minRequirementsTextView.setOnClickListener(view -> {
            // Handle click event
            Intent minReqIntent = new Intent(this, RequirementsActivity.class);
            minReqIntent.putExtra("requirement_type", "Minimum Requirements");
            minReqIntent.putExtra("description", minimumRequirements);
            minReqIntent.putExtra("poster_image_url", posterImage);
            startActivity(minReqIntent);
            // You can also navigate to another activity or perform other actions here
        });
        setTextViewMarquee(minRequirementsTextView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            minRequirementsTextView.setText(Html.fromHtml(minimumRequirements, Html.FROM_HTML_MODE_LEGACY));
        }
    }
    private void createMaxRequirements(String maximumRequirements, String posterImage) {
        TextView maxRequirementsTextView = findViewById(R.id.max_requirements);
        if(Objects.equals(maximumRequirements, "")) {
            maxRequirementsTextView.setVisibility(View.GONE);
            return;
        }
        maxRequirementsTextView.setOnClickListener(view -> {
            // Handle click event
            Intent recommendedReqIntent = new Intent(this, RequirementsActivity.class);
            recommendedReqIntent.putExtra("requirement_type", "Minimum Requirements");
            recommendedReqIntent.putExtra("description", maximumRequirements);
            recommendedReqIntent.putExtra("poster_image_url", posterImage);
            startActivity(recommendedReqIntent);
            // You can also navigate to another activity or perform other actions here
        });
        setTextViewMarquee(maxRequirementsTextView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            maxRequirementsTextView.setText(Html.fromHtml(maximumRequirements, Html.FROM_HTML_MODE_LEGACY));
        }
    }

    private static void setTextViewMarquee(TextView textView) {
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        textView.setSingleLine(true);
        textView.setMarqueeRepeatLimit(-1);  // -1 for infinite marquee
        textView.setHorizontallyScrolling(true);
        textView.setSelected(true); // Important for marquee
    }

    private void CreateAchievements(Intent intent) throws JSONException {
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
       // achievementsLayout.removeAllViews();
        // Loop through the arrays and create views for each achievement
        for (int i = 0; i < achievementNames.length(); i++) {
            String name = achievementNames.getString(i);
            String status = achievementStatuses.getString(i);
            String iconUrl = achievementIcons.getString(i);
            String desc =  achievementDesc.getString(i);
            // Create a horizontal LinearLayout to contain icon and texts
            LinearLayout achievementCard = CreateAchievementViews(intent, name, status, iconUrl, desc);
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
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
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

    private @NonNull LinearLayout CreateAchievementViews(Intent intent, String name, String status, String iconUrl, String desc) {

        LinearLayout achievementCard = createAchievementLayout(intent, name, status, iconUrl, desc);
        // Create ImageView for achievement icon
        ImageView iconImageView = CreateAchievementIconView(iconUrl);
        achievementCard.addView(iconImageView);

        // Create TextView for achievement name
        TextView nameTextView = CreateAchievementTextView(name, 11,LinearLayout.LayoutParams.WRAP_CONTENT);
        setTextViewMarquee(nameTextView);
        achievementCard.addView(nameTextView);

        // Create TextView for achievement status (unlocked/locked)
        TextView statusTextView = CreateAchievementTextView(status, 5, LinearLayout.LayoutParams.WRAP_CONTENT);
        //statusTextView.setPadding(20, 10, 20, 10); // Padding inside the TextView
        achievementCard.addView(statusTextView);

        // Create TextView for achievement description (unlocked/locked)
        TextView descTextView =  CreateAchievementTextView(desc, 8, 128);
        setTextViewMarquee(descTextView);
        achievementCard.addView(descTextView);
        return achievementCard;
    }

    private @NonNull LinearLayout createAchievementLayout(Intent intent, String name, String status, String iconUrl, String desc) {
        LinearLayout achievementCard = new LinearLayout(this);
        achievementCard.setFocusable(true);
        achievementCard.setFocusableInTouchMode(true);
        achievementCard.setBackgroundResource(R.drawable.achievement_card_selector);
        achievementCard.setOrientation(LinearLayout.VERTICAL);
        achievementCard.setPadding(8, 8, 8, 8); // Add padding between achievements
        // Create LayoutParams and set the margins
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                 124,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        linearLayoutParams.setMargins(2, 2, 2, 2); // Left, Top, Right, Bottom margins in pixels
        achievementCard.setLayoutParams(linearLayoutParams);
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
        return achievementCard;
    }
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeNowPlayingReceiver);
        // Unregister the BroadcastReceiver to avoid memory leaks
        LocalBroadcastManager.getInstance(this).unregisterReceiver(steamUpdateReceiver);
        // Unbind the service
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
        if(apex_bound){
            apex_bound = false;
        }
    }

}
