package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.ACTION_KODI_MOVIE_PLAYING;
import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.ACTION_MOVIE_RESUME_SLIDESHOW;
import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.ACTION_MOVIE_UPDATED;
import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.ACTION_PLEX_MOVIE_PLAYING;
import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.PLEX_BRIDGE_ADDRESS;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.Models.Movie;
import com.anthonybturner.cinemapostersanywhere.data.AppDatabase;
import com.anthonybturner.cinemapostersanywhere.data.MovieDao;
import com.anthonybturner.cinemapostersanywhere.interfaces.FetchCompleteCallback;
import com.anthonybturner.cinemapostersanywhere.services.MovieService;
import com.anthonybturner.cinemapostersanywhere.services.PostersApiService;
import com.anthonybturner.cinemapostersanywhere.services.WebSocketService;
import com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants;
import com.anthonybturner.cinemapostersanywhere.utilities.GenreUtils;
import com.anthonybturner.cinemapostersanywhere.utilities.ImageUtils;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {
    public static boolean isSameMovie(long movieId) {
        return lastDisplayedMovieId == movieId;
    }
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static long lastDisplayedMovieId = -1;
    private static boolean slideshowRunning;
    private TextView movieTitleTextView, movieOverviewTextView, movieCategoryTextView, movieRatingTextView, progressText, adultStatusTextView, originalLanguageTextView, genresTextView, popularityTextView, voteCountTextView, taglineTextView;
    private ImageView moviePosterImageView, tomatoIcon;
    private View btnCatNowPlaying;
    private ProgressBar progressBar;
    private Intent intentNowPlaying;
    private final Handler handler = new Handler();
    private PostersApiService movieApiService;
    private MovieDao movieDao;
    private Timer slideshowTimer;
    private List<Movie> movieList = new ArrayList<>();
    private int currentImageIndex = 0;
    private boolean showingNowPlaying = false;
    private boolean bound;
    private PowerManager.WakeLock wakeLock;
    private ActionBarDrawerToggle drawerToggle;
    private MutableLiveData<String> selectedCategory = new MutableLiveData<>();// LiveData for selected category
    private LiveData<List<Movie>> moviesByCategory;// LiveData for movies by category

    // Service Connection
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
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
        createButtons();
        initPlexWebSocket();
        initializeDatabase();
        registerReceivers();
        startServices();
    }

/************************************* UI Methods**********************************************************/
    private void initializeViews() {
        movieTitleTextView = findViewById(R.id.movie_title);
        moviePosterImageView = findViewById(R.id.movie_poster);
        movieOverviewTextView = findViewById(R.id.movie_overview);
        movieCategoryTextView = findViewById(R.id.movie_category);
        movieRatingTextView = findViewById(R.id.movie_tomato_percentage);
        taglineTextView = findViewById(R.id.movie_tagline);

        progressBar = findViewById(R.id.movie_progress_bar);
        //progressText = findViewById(R.id.movie_progress_text);
        //adultStatusTextView = findViewById(R.id.movie_ratings);
        originalLanguageTextView = findViewById(R.id.movie_country);
        genresTextView = findViewById(R.id.movie_genres);
       // popularityTextView = findViewById(R.id.movie_popularity);
       //voteCountTextView = findViewById(R.id.movie_vote_count);
        tomatoIcon = findViewById(R.id.movie_tomato_icon);
    }
    private void createButtons() {
        findViewById(R.id.btn_category_all).setOnClickListener(v -> observeAllMovies());
        findViewById(R.id.btn_category_top_rated).setOnClickListener(v -> onUpdateCategorySelection("Top Rated Movies"));
        findViewById(R.id.btn_category_popular).setOnClickListener(v -> onUpdateCategorySelection("Popular Movies"));
        findViewById(R.id.btn_category_trending).setOnClickListener(v -> onUpdateCategorySelection("Trending Movies"));
        findViewById(R.id.btn_delete).setOnClickListener(v -> onDeleteMovie());

        btnCatNowPlaying = findViewById(R.id.btn_category_now_playing);
        btnCatNowPlaying.setOnClickListener(v -> {
            handleNowPlayingIntent(intentNowPlaying);
        });
        findViewById(R.id.btn_settings).setOnClickListener(v ->  { Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }
    private void onDeleteMovie() {
        Movie currentMovie = movieList.get(currentImageIndex);
        Executors.newSingleThreadExecutor().execute(() -> {
            movieDao.deleteMovie(currentMovie.getId());
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Deleted movie: " + currentMovie.getTitle(), Toast.LENGTH_SHORT).show();
                movieList.remove(currentMovie);
                if (movieList.isEmpty()) {
                    stopSlideshow();
                }
            });
        });
    }
    public void startSlideshow() {
        slideshowTimer = new Timer();
        slideshowTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (showingNowPlaying || movieList.isEmpty()) return;
                    goNextSlide();
                });
            }
        }, 0, 15000);
        slideshowRunning = true;
    }
    private void resetSlideshowTimer() {
        // Stop the current slideshow
        stopSlideshow();
        // Start the slideshow again (new timer)
        startSlideshow();
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
        //progressText.setVisibility(View.GONE);
    }

    private void updateMovieDetails(Movie movie) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setMovieTitle(String.format("%s (%s)", movie.getTitle(), movie.getReleaseDateYear()));
        }
        setMovieOverview(movie.getOverview());
        setMovieCategory(movie.getCategory());
        setMovieRatings(movie.getVoteAverage());
        //setAdultStatus(movie.getAdult());
        setOriginalLanguage(movie.getOriginalLanguage());
        setGenres(movie.getGenres());
        //setPopularity(movie.getPopularity());
        //setVoteCount(movie.getVoteCount());
        loadPoster(movie);
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
            stopSlideshow();
            startSlideshow();
            Toast.makeText(MainActivity.this, "Posters resumed ", Toast.LENGTH_SHORT).show();
            slideshowRunning = true;
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
        updateMovieDetails(currentMovie);
    }

    public static boolean isSlideshowPlaying() {
        return slideshowRunning;
    }

    public void stopSlideshow() {
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
            slideshowTimer = null;
        }
        slideshowRunning = false;
    }

    private void updateNowPlayingButtonVisibility() {
        if(intentNowPlaying != null){
            btnCatNowPlaying.setVisibility(View.VISIBLE);
        }else{
            btnCatNowPlaying.setVisibility(View.GONE);
        }
    }

/************************************* Database and Observer Methods**********************************************************/

    // Initialize the Plex WebSocket, used for communication with railway server to get movie details
    private void initPlexWebSocket() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(PLEX_BRIDGE_ADDRESS)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        movieApiService = retrofit.create(PostersApiService.class);
    }

    private void initializeDatabase() {
        movieDao = AppDatabase.Companion.getInstance(this).movieDao();
        // Observe LiveData to fetch movies
        movieDao.getAllMovies().observe(this, new Observer<List<Movie>>() {
            @Override
            public void onChanged(List<Movie> localMovies) {
                if (localMovies != null && !localMovies.isEmpty()) {
                    movieList = localMovies;
                    onFetchCompleteCallback.onFetchComplete(null); // Call the callback here
                } else {
                    fetchServerPosters(onFetchCompleteCallback); // If no movies are found, fetch from server
                }
            }
        });
        // Observe the selected category and fetch movies when it changes
        selectedCategory.observe(this, new Observer<String>() {
            @Override
            public void onChanged(String category) {
                if (category != null) {
                    observeMoviesByCategory(category);
                }
            }
        });
    }
    private void observeAllMovies() {
        if (moviesByCategory != null) {
            moviesByCategory.removeObservers(this); // Remove previous observers
        }

        // Fetch LiveData for all movies
        moviesByCategory = movieDao.getAllMovies();
        moviesByCategory.observe(this, new Observer<List<Movie>>() {
            @Override
            public void onChanged(List<Movie> movies) {
                if (movies != null && !movies.isEmpty()) {
                    movieList = movies;
                    onFetchCompleteCallback.onFetchComplete(null);
                    Toast.makeText(MainActivity.this, "Fetched " + movies.size() + " movies from DB", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "No movies found locally. Fetching from server...", Toast.LENGTH_SHORT).show();
                    fetchServerPosters(onFetchCompleteCallback);
                }
            }
        });
    }

    private void observeMoviesByCategory(String category) {
        if (moviesByCategory != null) {
            moviesByCategory.removeObservers(this); // Remove previous observers
        }
        // Fetch LiveData for the new category
        moviesByCategory = movieDao.getMoviesByCategory(category);
        moviesByCategory.observe(this, new Observer<List<Movie>>() {
            @Override
            public void onChanged(List<Movie> movies) {
                if (movies != null && !movies.isEmpty()) {
                    movieList = movies;
                    Toast.makeText(MainActivity.this, "Fetched " + movies.size() + " " + category + " movies from DB", Toast.LENGTH_SHORT).show();
                    onFetchCompleteCallback.onFetchComplete(null);
                } else {
                    Toast.makeText(MainActivity.this, "No " + category + " movies found in the database.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void onUpdateCategorySelection(String category) {
        selectedCategory.setValue(category);
    }
    FetchCompleteCallback onFetchCompleteCallback = new FetchCompleteCallback() {// Callback for fetching movies
        @Override
        public void onFetchComplete(Intent intent) {
            runOnUiThread(() -> {
                //  hideProgress();
                stopSlideshow();
                startSlideshow();
            });
        }
    };
    private void registerReceivers() {
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(ACTION_PLEX_MOVIE_PLAYING));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(ACTION_KODI_MOVIE_PLAYING));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(ACTION_MOVIE_RESUME_SLIDESHOW));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(ACTION_MOVIE_UPDATED));
    }
    private final BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @SuppressLint("UnsafeIntentLaunch") Intent intent) {
            intentNowPlaying  = intent;
            String action = intent.getAction();
            if (ACTION_PLEX_MOVIE_PLAYING.equals(action) || ACTION_KODI_MOVIE_PLAYING.equals(action)) {
                handleNowPlayingIntent(intent);
            }else if(ACTION_MOVIE_RESUME_SLIDESHOW.equals(action)){
                Intent closeIntent = new Intent(MovieConstants.ACTION_KODI_MOVIE_CLOSING);
                LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(closeIntent);
                showingNowPlaying = false;
                intentNowPlaying = null;
                btnCatNowPlaying.setVisibility(View.GONE);
                resumeSlideshow();
            }else if (ACTION_MOVIE_UPDATED.equals(action)) {
                refreshLocalDatabase();
            }
        }
    };
    private void startServices() {
        Intent socketServiceIntent = new Intent(this, WebSocketService.class);
        startService(socketServiceIntent);

        Intent movieServiceIntent = new Intent(this, MovieService.class);
        startService(movieServiceIntent);
    }

    private void handleNowPlayingIntent(Intent intent) {
        lastDisplayedMovieId = intent.getLongExtra("id", -1);
        showNowPlaying(intent);
    }
    private void refreshLocalDatabase() {
        movieDao.deleteAllMovies();
    }

    private void fetchServerPosters(FetchCompleteCallback callback) {
        movieApiService.getPosters().enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processMovies(response.body());
                    callback.onFetchComplete(null);
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
            for (int i = 0; i < totalFiles; i++) {
                Movie movie = movieList.get(i);
                processMovie(movie, i, totalFiles);
            }
            movieDao.insertMovies(movieList);
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
        //updateProgress(index + 1, totalFiles);
    }

    /********************************************************* Events *****************************************************/
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
    protected void onResume(){
        super.onResume();
        updateNowPlayingButtonVisibility();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nowPlayingReceiver);
        stopService(new Intent(this, WebSocketService.class));
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }
}