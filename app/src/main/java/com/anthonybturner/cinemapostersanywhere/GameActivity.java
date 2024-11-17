package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.utilities.MovieConstants.PLEX_MOVIE_PLAYING_INTENT_ACTION;
import static com.anthonybturner.cinemapostersanywhere.utilities.MovieConstants.PLEX_BRIDGE_ADDRESS;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.Models.Game;
import com.anthonybturner.cinemapostersanywhere.Models.Video;
import com.anthonybturner.cinemapostersanywhere.interfaces.OnVideoDataFetchedListener;
import com.anthonybturner.cinemapostersanywhere.interfaces.TimerUpdateListener;
import com.anthonybturner.cinemapostersanywhere.services.PostersApiService;
import com.anthonybturner.cinemapostersanywhere.services.SteamGameService;
import com.anthonybturner.cinemapostersanywhere.services.WebSocketService;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class GameActivity extends AppCompatActivity implements OnVideoDataFetchedListener, TimerUpdateListener {

    public static final String Game_UPDATED_INTENT_ACTION = "";
    public static final String STEAM_GAME_PLAYING_ACTION = "STEAM_GAME_UPDATE";
    public static final String CLOSE_NOW_PLAYING_ACTION = "com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING";
    public static final String APEX_LEGENDS_API_UPDATE_ACTION = "com.anthonybturner.cinemapostersanywhere.STEAM_UPDATE";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private TextView GameTitleTextView, GameOverviewTextView, GameCategoryTextView, GameRatingTextView,
            progressText, adultStatusTextView, originalLanguageTextView, genresTextView, popularityTextView, voteCountTextView;
    private ImageView GamePosterImageView, tomatoIcon;
    private ProgressBar progressBar;
    private final Handler handler = new Handler();
    private Timer slideshowTimer;
    private int currentImageIndex = 0;
    private static String lastDisplayedGameId = null;
    private boolean showingNowPlaying = false;
    private boolean isUsingSteamGameSystem = false;
    private List<Game> gameList = new ArrayList<>();
    private PostersApiService GameApiService;
    private static boolean slideshowRunning;
    private PowerManager.WakeLock wakeLock;
    private SteamGameService steamGameService;
    private boolean bound;
    private ActionBarDrawerToggle drawerToggle;

    // Service Connection
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SteamGameService.LocalBinder binder = (SteamGameService.LocalBinder) service;
            steamGameService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        initializeViews();
        createButtons();
        //createDrawer();
        initializeRetrofit();
        initializeDatabase();
        fetchGames();
        registerReceivers();
        startServices();
        isUsingSteamGameSystem = true;
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

    private void initializeViews() {
        GameTitleTextView = findViewById(R.id.game_title);
        GamePosterImageView = findViewById(R.id.game_poster);
        GameOverviewTextView = findViewById(R.id.game_overview);
        GameCategoryTextView = findViewById(R.id.game_category);
        GameRatingTextView = findViewById(R.id.game_tomato_percentage);
        progressBar = findViewById(R.id.game_progress_bar);
        progressText = findViewById(R.id.game_progress_text);
        adultStatusTextView = findViewById(R.id.game_ratings);
        originalLanguageTextView = findViewById(R.id.game_og_lang);
        genresTextView = findViewById(R.id.game_genres);
        popularityTextView = findViewById(R.id.game_popularity);
        voteCountTextView = findViewById(R.id.game_vote_count);
        tomatoIcon = findViewById(R.id.game_tomato_icon);
    }

    private void createButtons() {
        findViewById(R.id.button_all).setOnClickListener(v -> fetchGames());
        findViewById(R.id.button_top_rated).setOnClickListener(v -> fetchGamesByCategory("Top Games"));
        findViewById(R.id.button_popular).setOnClickListener(v -> fetchGamesByCategory("Popular Games"));
        findViewById(R.id.button_trending).setOnClickListener(v -> fetchGamesByCategory("Trending Games"));
        findViewById(R.id.button_settings).setOnClickListener(v -> {
            Intent intent = new Intent(GameActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        Button steamButton = findViewById(R.id.button_steam);
        steamButton.setOnClickListener(v -> {
            if (steamGameService != null) {
                steamGameService.checkCurrentGame(true);
            }
        });
        hideSteamButton();
    }

    private void initializeRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PLEX_BRIDGE_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        GameApiService = retrofit.create(PostersApiService.class);
    }

    private void initializeDatabase() {
       // gameDAO = AppDatabase.Companion.getInstance(this).GameDao();
    }

    private void registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(PLEX_MOVIE_PLAYING_INTENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(Game_UPDATED_INTENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(STEAM_GAME_PLAYING_ACTION));
    }

    private void startServices() {
        Intent socketServiceIntent = new Intent(this, WebSocketService.class);
        startService(socketServiceIntent);
        // Start the SteamGameService
        Intent serviceIntent = new Intent(this, SteamGameService.class);
        startService(serviceIntent);

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private final BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @SuppressLint("UnsafeIntentLaunch") Intent intent) {
            String action = intent.getAction();
            if (STEAM_GAME_PLAYING_ACTION.equals(action) && isUsingSteamGameSystem) {
                handleGamePlaying(intent);
            } else if (APEX_LEGENDS_API_UPDATE_ACTION.equals(action)) {
                handleApexLegendsPlaying(intent);
            }
        }
    };

    private void handleApexLegendsPlaying(Intent intent) {

    }

    private void handleGamePlaying(Intent intent) {
        String status = intent.getStringExtra("game_status");
        if (Objects.equals(status, "success")) {
            // Check if the new game ID is different from the last displayed
            lastDisplayedGameId = intent.getStringExtra("game_id");
            //stopSlideshow();
            showingNowPlaying = true;
            intent.setClass(this, SteamGameActivity.class);
            startActivity(intent);
        } else if (Objects.equals(status, "failed")) {
            lastDisplayedGameId = "";
            resumeSlideshowFromNowPlaying();
        }
    }

    public static boolean isSameGameDisplayed(String gameId) {
        return gameId != null && gameId.equals(lastDisplayedGameId);
    }

    private void handleNowPlayingIntent(Intent intent) {
        String action = intent.getStringExtra("action");
        if ("now_playing".equals(action)) {
           // showNowPlaying(intent);
        } else if ("resume_slideshow".equals(action)) {
            resumeSlideshowFromNowPlaying();
        }
    }

    private void resumeSlideshowFromNowPlaying() {
        Intent closeIntent = new Intent(CLOSE_NOW_PLAYING_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(closeIntent);
        showingNowPlaying = false;
       // resumeSlideshow();
    }

    private void refreshLocalDatabase() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
           // gameDAO.deleteAllGames();
           // fetchServerGames();
        });
    }

    private void fetchGamesByCategory(String category) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Game> Games = null;//gameDAO.getGamesByCategory(category);
            runOnUiThread(() -> {
                if (!Games.isEmpty()) {
                    gameList = Games;
                    Toast.makeText(GameActivity.this, "Fetched " + Games.size() + " " + category + " from DB", Toast.LENGTH_SHORT).show();
                    //startSlideshow();
                } else {
                    Toast.makeText(GameActivity.this, "No " + category + " found in the database.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void fetchGames() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Game> localGames = null;///gameDAO.getAllGames();
            runOnUiThread(() -> {
                if (!localGames.isEmpty()) {
                    gameList = localGames;
                    Toast.makeText(GameActivity.this, "Fetched " + gameList.size() + " Games from DB", Toast.LENGTH_SHORT).show();
                   // hideProgress();
                  //  startSlideshow();
                } else {
                   // fetchServerGames();
                }
            });
        });
    }

    private void showProgress(int totalFiles) {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            progressBar.setMax(totalFiles);
        });
    }

    private void updateProgress(int currentFile, int totalFiles) {
        runOnUiThread(() -> {
            progressBar.setProgress(currentFile);
            int percentage = (int) ((currentFile / (float) totalFiles) * 100);
            progressText.setText(String.format("Loading posters...\n%d/%d (%d%%)", currentFile, totalFiles, percentage));
        });
    }

    private void hideProgress() {
        progressBar.setVisibility(View.GONE);
        progressText.setVisibility(View.GONE);
    }

    public void startSlideshow() {
        stopSlideshow();
        slideshowRunning = true;
        slideshowTimer = new Timer();
        slideshowTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (showingNowPlaying || gameList.isEmpty()) return;
                    //currentImageIndex = getRandomGameIndex(flaskGameList.size());
                    goNextSlide();
                });
            }
        }, 0, 15000);
    }

    private int getRandomGameIndex(int GameListSize) {
        if (currentImageIndex >= GameListSize) {
            currentImageIndex = 0;
        }
        return new Random().nextInt(GameListSize);
    }

    private void loadPoster(Game Game) {
        String posterPath = Game.getPosterImage();
        if (posterPath != null && !posterPath.isEmpty()) {
            Glide.with(GameActivity.this)
                    .load(new File(posterPath))
                    .centerCrop()
                    .into(GamePosterImageView);
        } else {
            Glide.with(GameActivity.this)
                    .load(Game.getPosterPath())
                    .centerCrop()
                    .into(GamePosterImageView);
        }
    }

    private void updateGameDetails(Game Game) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setGameTitle(String.format("%s (%s)", Game.getTitle(), Game.getReleaseDateYear()));
        }
        setGameOverview(Game.getOverview());
        setGameCategory(Game.getCategory());
        setGameRatings(Game.getVoteAverage());
        setAdultStatus(Game.getAdult());
        setOriginalLanguage(Game.getOriginalLanguage());
        setGenres(Game.getGenres());
        setPopularity(Game.getPopularity());
        setVoteCount(Game.getVoteCount());
    }

    private void setAdultStatus(Boolean isAdult) {
        adultStatusTextView.setText(isAdult ? "[Adult]" : "[Family-friendly]");
    }

    private void setOriginalLanguage(String language) {
        originalLanguageTextView.setText(language != null ? String.format("Language: [%s]", language) : "Language: [Unknown Language]");
    }

    private void setGenres(String genres) {
        genresTextView.setText(genres);
    }

    private void setPopularity(double popularity) {
        popularityTextView.setText(String.format("Popularity: [%.2f]", popularity));
    }

    private void setVoteCount(int voteCount) {
        voteCountTextView.setText(String.format("Vote Count: [%d]", voteCount));
    }

    private void setGameRatings(float tomatoRating) {
        if (tomatoIcon == null) {
            Log.e("MainActivity", "Tomato icon ImageView is not initialized");
            return;
        }
        float scaledRating = tomatoRating * 10;
        tomatoIcon.setImageResource(scaledRating >= 60.0 ? R.drawable.fresh_tomato : R.drawable.rotten_tomato);
        String formattedRating = String.format("Rating: %.1f", scaledRating);
        GameRatingTextView.setText(formattedRating + "%");
    }

    private void setGameCategory(String category) {
        GameCategoryTextView.setText(category);
    }

    private void setGameOverview(String overview) {
        GameOverviewTextView.setText(overview);
    }

    public void setGameTitle(String title) {
        GameTitleTextView.setText(title);
    }

    public void showNowPlaying(Intent intent) {
        stopSlideshow();
        showingNowPlaying = true;
        intent.setClass(this, NowPlayingActivity.class);
        startActivity(intent);
    }

    public void resumeSlideshow() {
        if (!showingNowPlaying) {
            startSlideshow();
            Toast.makeText(GameActivity.this, "Posters resumed ", Toast.LENGTH_SHORT).show();
        }
    }

    private void goBackSlide() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
        } else {
            currentImageIndex = gameList.size() - 1;
        }
        showCurrentSlide();
    }

    private void goNextSlide() {
        if (currentImageIndex < gameList.size() - 1) {
            currentImageIndex++;
        } else {
            currentImageIndex = 0;
        }
        showCurrentSlide();
    }

    private void showCurrentSlide() {
        Game currentGame = gameList.get(currentImageIndex);
        loadPoster(currentGame);
        updateGameDetails(currentGame);
    }

    public static boolean isSlideshowPlaying() {
        return slideshowRunning;
    }

    public void stopSlideshow() {
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                stopSlideshow();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                if (isSlideshowPlaying()) {
                    stopSlideshow();
                } else {
                    resumeSlideshow();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                goBackSlide();
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                goNextSlide();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nowPlayingReceiver);
        stopService(new Intent(this, WebSocketService.class));
        stopService(new Intent(this, SteamGameService.class));
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    @Override
    public void onVideoDataFetched(List<Video> videoList) {

    }

    @Override
    public void onTimerUpdate(long durationInMillis, boolean isRanked, boolean isArenas) {

    }

    @Override
    public void onTimerFinish(boolean isRanked) {
        hideSteamButton();
    }

    @Override
    public void onTimerStarted(Intent intent, boolean isRanked, boolean isArenas){
        showSteamButton();
    }

    @Override
    public void onPlayerStatsTimerStarted(Intent intent, boolean isRanked) {

    }

    private void hideSteamButton() {
        Button button = findViewById(R.id.button_steam);
        button.setEnabled(false);
        button.setVisibility(View.GONE);
    }

    private void showSteamButton() {
        Button button = findViewById(R.id.button_steam);
        button.setEnabled(true);
        button.setVisibility(View.VISIBLE);
    }
}