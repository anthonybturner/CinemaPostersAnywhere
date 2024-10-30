package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.Models.Movie;
import com.anthonybturner.cinemapostersanywhere.Models.Video;
import com.anthonybturner.cinemapostersanywhere.interfaces.OnVideoDataFetchedListener;
import com.anthonybturner.cinemapostersanywhere.interfaces.TimerUpdateListener;
import com.anthonybturner.cinemapostersanywhere.services.PostersApiService;
import com.anthonybturner.cinemapostersanywhere.data.MovieDao;
import com.anthonybturner.cinemapostersanywhere.data.AppDatabase;
import com.anthonybturner.cinemapostersanywhere.services.WebSocketService;
import com.anthonybturner.cinemapostersanywhere.services.SteamGameService;
import com.anthonybturner.cinemapostersanywhere.utilities.GenreUtils;
import com.anthonybturner.cinemapostersanywhere.utilities.ImageUtils;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnVideoDataFetchedListener, TimerUpdateListener {

    public static final String STEAM_GAME_PLAYING_ACTION = "STEAM_GAME_UPDATE";
    public static final String CLOSE_NOW_PLAYING_ACTION = "com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING";
    public static final String APEX_LEGENDS_API_UPDATE_ACTION = "com.anthonybturner.cinemapostersanywhere.STEAM_UPDATE";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private TextView movieTitleTextView, movieOverviewTextView, movieCategoryTextView, movieRatingTextView,
            progressText, adultStatusTextView, originalLanguageTextView, genresTextView, popularityTextView, voteCountTextView;
    private ImageView moviePosterImageView, tomatoIcon;
    private ProgressBar progressBar;
    private final Handler handler = new Handler();
    private Timer slideshowTimer;
    private int currentImageIndex = 0;
    private static String lastDisplayedGameId = null;
    private boolean showingNowPlaying = false;
    private boolean isUsingSteamGameSystem = false;
    private List<Movie> movieList = new ArrayList<>();
    private PostersApiService movieApiService;
    private MovieDao movieDao;
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
            steamGameService.setTimerCallback(MainActivity.this);
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
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeWakeLock();
        createButtons();
        //createDrawer();
        initializeRetrofit();
        initializeDatabase();
        fetchMovies();
        registerReceivers();
        startServices();
        isUsingSteamGameSystem = true;
        // Request notification permission if needed (only for Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // TIRAMISU corresponds to API 33
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can proceed with notifications
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    private void initializeViews() {
        movieTitleTextView = findViewById(R.id.movie_title);
        moviePosterImageView = findViewById(R.id.movie_poster);
        movieOverviewTextView = findViewById(R.id.movie_overview);
        movieCategoryTextView = findViewById(R.id.movie_category);
        movieRatingTextView = findViewById(R.id.movie_tomato_percentage);
        progressBar = findViewById(R.id.movie_progress_bar);
        progressText = findViewById(R.id.movie_progress_text);
        adultStatusTextView = findViewById(R.id.movie_ratings);
        originalLanguageTextView = findViewById(R.id.movie_og_lang);
        genresTextView = findViewById(R.id.movie_genres);
        popularityTextView = findViewById(R.id.movie_popularity);
        voteCountTextView = findViewById(R.id.movie_vote_count);
        tomatoIcon = findViewById(R.id.movie_tomato_icon);
    }

    private void initializeWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyApp::WakeLockTag");
        wakeLock.acquire();
    }

    private void createButtons() {
        findViewById(R.id.button_all).setOnClickListener(v -> fetchMovies());
        findViewById(R.id.button_top_rated).setOnClickListener(v -> fetchMoviesByCategory("Top Movies"));
        findViewById(R.id.button_popular).setOnClickListener(v -> fetchMoviesByCategory("Popular Movies"));
        findViewById(R.id.button_trending).setOnClickListener(v -> fetchMoviesByCategory("Trending Movies"));
        findViewById(R.id.button_settings).setOnClickListener(v ->  { Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
        Button steamButton = findViewById(R.id.button_steam);
        steamButton.setOnClickListener(v ->  {
            if(steamGameService != null){
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
        movieApiService = retrofit.create(PostersApiService.class);
    }

    private void initializeDatabase() {
        movieDao = AppDatabase.Companion.getInstance(this).movieDao();
    }

    private void registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(NOW_PLAYING_INTENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(MOVIE_UPDATED_INTENT_ACTION));
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
            if (NOW_PLAYING_INTENT_ACTION.equals(action)) {
                handleNowPlayingIntent(intent);
            } else if (MOVIE_UPDATED_INTENT_ACTION.equals(action)) {
                refreshLocalDatabase();
            }else if (STEAM_GAME_PLAYING_ACTION.equals(action) && isUsingSteamGameSystem) {
               handleGamePlaying(intent);
            }else if (APEX_LEGENDS_API_UPDATE_ACTION.equals(action)) {
                handleApexLegendsPlaying(intent);
            }
        }
    };

    private void handleApexLegendsPlaying(Intent intent) {

    }

    private void handleGamePlaying(Intent intent) {
        String status = intent.getStringExtra("game_status");
        if(Objects.equals(status, "success")){
            // Check if the new game ID is different from the last displayed
            lastDisplayedGameId = intent.getStringExtra("game_id");
            stopSlideshow();
            showingNowPlaying = true;
            intent.setClass(this, SteamGameActivity.class);
            startActivity(intent);
        }else if(Objects.equals(status, "failed")){
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
            showNowPlaying(intent);
        } else if ("resume_slideshow".equals(action)) {
            resumeSlideshowFromNowPlaying();
        }
    }

    private void resumeSlideshowFromNowPlaying() {
        Intent closeIntent = new Intent(CLOSE_NOW_PLAYING_ACTION);
        LocalBroadcastManager.getInstance(this).sendBroadcast(closeIntent);
        showingNowPlaying = false;
        resumeSlideshow();
    }

    private void refreshLocalDatabase() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            movieDao.deleteAllMovies();
            fetchServerMovies();
        });
    }

    private void fetchMoviesByCategory(String category) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Movie> movies = movieDao.getMoviesByCategory(category);
            runOnUiThread(() -> {
                if (!movies.isEmpty()) {
                    movieList = movies;
                    Toast.makeText(MainActivity.this, "Fetched " + movies.size() + " " + category + " from DB", Toast.LENGTH_SHORT).show();
                    startSlideshow();
                } else {
                    Toast.makeText(MainActivity.this, "No " + category + " found in the database.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void fetchMovies() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Movie> localMovies = movieDao.getAllMovies();
            runOnUiThread(() -> {
                if (!localMovies.isEmpty()) {
                    movieList = localMovies;
                    Toast.makeText(MainActivity.this, "Fetched " + movieList.size() + " Movies from DB", Toast.LENGTH_SHORT).show();
                    hideProgress();
                    startSlideshow();
                } else {
                    fetchServerMovies();
                }
            });
        });
    }

    private void fetchServerMovies() {
        movieApiService.getPosters().enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processMovies(response.body());
                } else {
                    hideProgress();
                }
            }
            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e("MainActivity", "Failed to fetch movies from Flask API", t);
                hideProgress();
            }
        });
    }

    private void processMovies(List<Movie> movies) {
        movieList = movies;
        int totalFiles = movieList.size();
        showProgress(totalFiles);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            for (int i = 0; i < totalFiles; i++) {
                Movie movie = movieList.get(i);
                processMovie(movie, i, totalFiles);
            }
            movieDao.insertMovies(movieList);
            runOnUiThread(() -> {
                hideProgress();
                startSlideshow();
            });
        });
    }

    private void processMovie(Movie movie, int index, int totalFiles) {
        if (movie.getGenreIds() != null) {
            String genreNames = GenreUtils.convertGenreIdsToNames(movie.getGenreIds());
            movie.setGenres(genreNames);
        }
        Bitmap bitmap = ImageUtils.downloadImage(movie.getPosterPath());
        if (bitmap != null) {
            //Change tmdb api poster path to local image path.
            String imagePath = ImageUtils.saveImageToStorage(this, bitmap, movie.getTitle() + ".jpg");
            movie.setPosterImage(imagePath);
        }
        updateProgress(index + 1, totalFiles);
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
                    if (showingNowPlaying || movieList.isEmpty()) return;
                    //currentImageIndex = getRandomMovieIndex(flaskMovieList.size());
                    goNextSlide();
                });
            }
        }, 0, 15000);
    }

    private int getRandomMovieIndex(int movieListSize) {
        if (currentImageIndex >= movieListSize) {
            currentImageIndex = 0;
        }
        return new Random().nextInt(movieListSize);
    }

    private void loadPoster(Movie movie) {
        String posterPath = movie.getPosterImage();
        if (posterPath != null && !posterPath.isEmpty()) {
            Glide.with(MainActivity.this)
                    .load(new File(posterPath))
                    .centerCrop()
                    .into(moviePosterImageView);
        } else {
            Glide.with(MainActivity.this)
                    .load(movie.getPosterPath())
                    .centerCrop()
                    .into(moviePosterImageView);
        }
    }

    private void updateMovieDetails(Movie movie) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setMovieTitle(String.format("%s (%s)", movie.getTitle(), movie.getReleaseDateYear()));
        }
        setMovieOverview(movie.getOverview());
        setMovieCategory(movie.getCategory());
        setMovieRatings(movie.getVoteAverage());
        setAdultStatus(movie.getAdult());
        setOriginalLanguage(movie.getOriginalLanguage());
        setGenres(movie.getGenres());
        setPopularity(movie.getPopularity());
        setVoteCount(movie.getVoteCount());
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

    private void setMovieRatings(float tomatoRating) {
        if (tomatoIcon == null) {
            Log.e("MainActivity", "Tomato icon ImageView is not initialized");
            return;
        }
        float scaledRating = tomatoRating * 10;
        tomatoIcon.setImageResource(scaledRating >= 60.0 ? R.drawable.fresh_tomato : R.drawable.rotten_tomato);
        String formattedRating = String.format("Rating: %.1f", scaledRating);
        movieRatingTextView.setText(formattedRating + "%");
    }

    private void setMovieCategory(String category) {
        movieCategoryTextView.setText(category);
    }

    private void setMovieOverview(String overview) {
        movieOverviewTextView.setText(overview);
    }

    public void setMovieTitle(String title) {
        movieTitleTextView.setText(title);
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
            Toast.makeText(MainActivity.this, "Posters resumed ", Toast.LENGTH_SHORT).show();
        }
    }

    private void goBackSlide() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
        } else {
            currentImageIndex = movieList.size() - 1;
        }
        showCurrentSlide();
    }

    private void goNextSlide() {
        if (currentImageIndex < movieList.size() - 1) {
            currentImageIndex++;
        } else {
            currentImageIndex = 0;
        }
        showCurrentSlide();
    }

    private void showCurrentSlide() {
        Movie currentMovie = movieList.get(currentImageIndex);
        loadPoster(currentMovie);
        updateMovieDetails(currentMovie);
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
    public void onTimerStarted() {
        showSteamButton();
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