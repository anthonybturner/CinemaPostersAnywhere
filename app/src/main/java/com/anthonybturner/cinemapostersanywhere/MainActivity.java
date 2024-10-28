package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.*;

import android.annotation.SuppressLint;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.Models.Movie;
import com.anthonybturner.cinemapostersanywhere.Models.Video;
import com.anthonybturner.cinemapostersanywhere.interfaces.OnVideoDataFetchedListener;
import com.anthonybturner.cinemapostersanywhere.services.FlaskApiService;
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

public class MainActivity extends AppCompatActivity implements OnVideoDataFetchedListener {

    public static final String STEAM_GAME_PLAYING_ACTION = "STEAM_GAME_UPDATE";
    public static final String CLOSE_NOW_PLAYING_ACTION = "com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING";
    public static final String APEX_LEGENDS_API_UPDATE_ACTION = "com.anthonybturner.cinemapostersanywhere.STEAM_UPDATE";
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
    private FlaskApiService movieApiService;
    private MovieDao movieDao;
    private static boolean slideshowRunning;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeWakeLock();
        createButtons();
        initializeRetrofit();
        initializeDatabase();
        fetchMovies();
        registerReceivers();
        startServices();
        isUsingSteamGameSystem = true;
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
    }

    private void initializeRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PLEX_BRIDGE_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        movieApiService = retrofit.create(FlaskApiService.class);
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
        Intent serviceIntent = new Intent(this, WebSocketService.class);
        startService(serviceIntent);

        // Start the SteamGameService
        Intent intent = new Intent(this, SteamGameService.class);
        startService(intent);
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
    }

    @Override
    public void onVideoDataFetched(List<Video> videoList) {

    }
}