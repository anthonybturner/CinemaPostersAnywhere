package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.MOVIE_UPDATED_INTENT_ACTION;
import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.NOW_PLAYING_INTENT_ACTION;
import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.PLEX_BRIDGE_ADDRESS;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.Intent;
import android.content.BroadcastReceiver;

import com.anthonybturner.cinemapostersanywhere.Models.Movie;
import com.anthonybturner.cinemapostersanywhere.services.FlaskApiService;
import com.anthonybturner.cinemapostersanywhere.data.MovieDao;
import com.anthonybturner.cinemapostersanywhere.data.AppDatabase; // Import AppDatabase
import com.anthonybturner.cinemapostersanywhere.utilities.GenreUtils;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import com.anthonybturner.cinemapostersanywhere.services.WebSocketService;

public class MainActivity extends AppCompatActivity {

    private TextView movieTitleTextView;
    private TextView movieOverviewTextView;
    private TextView movieCategoryTextView;
    private TextView movieRatingTextView;
    private ImageView moviePosterImageView;
    private TextView adultStatusTextView;
    private TextView originalLanguageTextView;
    private TextView genresTextView;
    private TextView popularityTextView;
    private TextView voteCountTextView;
    private ProgressBar progressBar;  // Progress bar for loading state
    private TextView progressText;
    private ImageView tomatoIcon; // ImageView for tomato rating icon
    private final Handler handler = new Handler();
    private Timer slideshowTimer;
    private int currentImageIndex = 0;
    private boolean showingNowPlaying = false;  // Flag to switch between slideshow and Now Playing
    private List<Movie> flaskMovieList = new ArrayList<>();  // For storing movie data from Flask API
    private Retrofit retrofit;
    private FlaskApiService flaskApiService; // Flask API service
    private MovieDao movieDao;  // DAO for local database
    private boolean slideshowRunning;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views for displaying title, year, poster, and progress bar
        movieTitleTextView = findViewById(R.id.movie_title);
        moviePosterImageView = findViewById(R.id.movie_poster);
        movieOverviewTextView = findViewById(R.id.movie_overview);
        movieCategoryTextView = findViewById(R.id.movie_category);
        movieRatingTextView = findViewById(R.id.tomato_percentage);
        progressBar = findViewById(R.id.progressBar); // Progress bar initialization
        progressText = findViewById(R.id.progressText);
        adultStatusTextView = findViewById(R.id.adult_status);
        originalLanguageTextView = findViewById(R.id.original_language);
        genresTextView = findViewById(R.id.genres);
        popularityTextView = findViewById(R.id.popularity);
        voteCountTextView = findViewById(R.id.vote_count);
        // Initialize the ImageView for the tomato icon
        tomatoIcon = findViewById(R.id.tomato_icon);


        // Get PowerManager system service
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

        // Create a WakeLock with the PARTIAL_WAKE_LOCK level to keep the CPU running
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "MyApp::WakeLockTag");
        // Acquire the WakeLock
        wakeLock.acquire();
        createButtons();

        // Initialize Retrofit for Flask API calls
        retrofit = new Retrofit.Builder()
                .baseUrl(PLEX_BRIDGE_ADDRESS)  // Flask API base URL from Constants
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        flaskApiService = retrofit.create(FlaskApiService.class); // Create FlaskApiService instance

        // Initialize the DAO for local database access
        movieDao = AppDatabase.Companion.getInstance(this).movieDao(); // Use AppDatabase to get MovieDao

        // Fetch movies from local database or Flask API
        fetchMovies();

        // Register the receiver for Now Playing events
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(NOW_PLAYING_INTENT_ACTION));
        LocalBroadcastManager.getInstance(this).registerReceiver(nowPlayingReceiver,
                new IntentFilter(MOVIE_UPDATED_INTENT_ACTION));

        // Start the WebSocketService
        Intent serviceIntent = new Intent(this, WebSocketService.class);
        startService(serviceIntent);
    }

    private final BroadcastReceiver nowPlayingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), NOW_PLAYING_INTENT_ACTION)) {
                if (Objects.equals(intent.getStringExtra("action"), "now_playing")) {
                    String title = intent.getStringExtra("title");
                    String year = intent.getStringExtra("year");
                    String overview = intent.getStringExtra("overview");
                    String posterUrl = intent.getStringExtra("posterUrl");

                    // Update UI with now playing info
                    assert year != null;
                    showNowPlaying(title, year, posterUrl, overview);
                } else if (Objects.equals(intent.getStringExtra("action"), "resume_slideshow")) {
                    // Send broadcast to close NowPlayingActivity
                    Intent closeIntent = new Intent("com.anthonybturner.cinemapostersanywhere.CLOSE_NOW_PLAYING");
                    LocalBroadcastManager.getInstance(context).sendBroadcast(closeIntent);
                    showingNowPlaying = false;
                    resumeSlideshow();
                }
            } else if (Objects.equals(intent.getAction(), MOVIE_UPDATED_INTENT_ACTION)) {
                String movie_status = intent.getStringExtra("movieStatus");
                runOnUiThread(() -> refreshLocalDatabase());
            }
        }
    };

    private void createButtons() {
        // Set click listeners for each button
        Button buttonAll = findViewById(R.id.button_all);
        Button buttonTopRated = findViewById(R.id.button_top_rated);
        Button buttonPopular = findViewById(R.id.button_popular);
        Button buttonTrending = findViewById(R.id.button_trending);

        // Handle All Movies button click
        buttonAll.setOnClickListener(v -> fetchMovies());

        // Handle Top Rated button click
        buttonTopRated.setOnClickListener(v -> fetchMoviesByCategory("Top Movies"));

        // Handle Popular button click
        buttonPopular.setOnClickListener(v -> fetchMoviesByCategory("Popular Movies"));

        // Handle Trending button click
        buttonTrending.setOnClickListener(v -> fetchMoviesByCategory("Trending Movies"));
    }
    private void refreshLocalDatabase() {
        // Clear the local database
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            movieDao.deleteAllMovies();  // Delete all movies from the local Room database
            // Fetch new movie data from the Flask API
            fetchMoviesFromFlask();  // This method will repopulate the local database with new movies
        });
    }
    private void fetchMoviesByCategory(String category) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // Query movies from the local database based on the provided category
            List<Movie> movies = movieDao.getMoviesByCategory(category);
            // Update the UI on the main thread
            runOnUiThread(() -> {
                if (!movies.isEmpty()) {
                    flaskMovieList = movies;
                    Toast.makeText(MainActivity.this, "Fetched " + movies.size() + " " + category + " from DB", Toast.LENGTH_SHORT).show();
                    startSlideshow();  // Start the slideshow with the fetched movies
                } else {
                    Toast.makeText(MainActivity.this, "No " + category + "  found in the database.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // Fetch movies from local database or Flask API
    private void fetchMovies() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Movie> localMovies = movieDao.getAllMovies();
            runOnUiThread(() -> {
                if (!localMovies.isEmpty()) {
                    flaskMovieList = localMovies;  // Use local movies if available
                    Toast.makeText(MainActivity.this, "Fetched " + flaskMovieList.size() + " Movies from DB", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    startSlideshow();
                } else {
                    fetchMoviesFromFlask();  // No movies in local DB, fetch from Flask API
                }
            });
        });
    }

    private void fetchMoviesFromFlask() {
        flaskApiService.getPosters().enqueue(new Callback<List<Movie>>() {
            @Override
            public void onResponse(Call<List<Movie>> call, Response<List<Movie>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    flaskMovieList = response.body();
                    int totalFiles = flaskMovieList.size();
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.VISIBLE);
                        progressText.setVisibility(View.VISIBLE);
                        progressBar.setMax(totalFiles);
                    });

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    executor.execute(() -> {
                        for (int i = 0; i < totalFiles; i++) {
                            Movie movie = flaskMovieList.get(i);

                            // Convert genre IDs to genre names
                            if (movie.getGenreIds() != null) {
                                String genreNames = GenreUtils.convertGenreIdsToNames(movie.getGenreIds()); // Converts genre IDs to names
                                movie.setGenres(genreNames);  // Set the genres field in the Movie object
                            }

                            // Download and save the poster image to storage
                            Bitmap bitmap = downloadImage(movie.getPosterPath());
                            if (bitmap != null) {
                                String imagePath = saveImageToStorage(bitmap, movie.getTitle() + ".jpg");
                                movie.setPosterImage(imagePath);
                            }

                            // Update progress on the UI thread
                            final int currentFile = i + 1;
                            runOnUiThread(() -> {
                                // Update progress
                                progressBar.setProgress(currentFile);
                                // Calculate percentage
                                int percentage = (int) ((currentFile / (float) totalFiles) * 100);
                                // Update progress text
                                progressText.setText(String.format("Loading posters...\n%d/%d (%d%%)", currentFile, totalFiles, percentage));
                            });
                        }

                        // Insert movies into the database and start the slideshow
                        movieDao.insertMovies(flaskMovieList);

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            progressText.setVisibility(View.GONE);
                            startSlideshow();
                        });
                    });
                } else {
                    // Hide progress if response fails
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        progressText.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onFailure(Call<List<Movie>> call, Throwable t) {
                Log.e("MainActivity", "Failed to fetch movies from Flask API", t);
                // Hide progress in case of failure
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                });
            }
        });
    }

    // Method to start the Flask movie slideshow
    public void startSlideshow() {
        stopSlideshow();  // Ensure any existing slideshow is stopped
        slideshowRunning = true;
        slideshowTimer = new Timer();
        slideshowTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> {
                    if (showingNowPlaying || flaskMovieList.isEmpty()) return;

                    currentImageIndex = getRandomMovieIndex(flaskMovieList.size());
                    Movie currentMovie = flaskMovieList.get(currentImageIndex);

                    loadPoster(currentMovie);
                    updateMovieDetails(currentMovie);

                    currentImageIndex++;
                });
            }
        }, 0, 15000);  // Change slides every 15 seconds
    }

    // Method to get a random index from the movie list
    private int getRandomMovieIndex(int movieListSize) {
        if (currentImageIndex >= movieListSize) {
            currentImageIndex = 0;
        }
        return new Random().nextInt(movieListSize);
    }

    // Method to load the poster (from file or URL)
    private void loadPoster(Movie movie) {
        String posterPath = movie.getPosterImage();  // Get the file path of the image

        if (posterPath != null && !posterPath.isEmpty()) {
            // Load the image from the file path using Glide
            Glide.with(MainActivity.this)
                    .load(new File(posterPath))
                    .centerCrop()
                    .into(moviePosterImageView);
        } else {
            Glide.with(MainActivity.this)
                    .load(movie.getPosterPath())  // Load from URL if no file path available
                    .centerCrop()
                    .into(moviePosterImageView);
        }
    }

    // Method to download an image and return it as a Bitmap
    private Bitmap downloadImage(String posterPath) {
        InputStream inputStream = null;
        try {
            URL url = new URL(posterPath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();
            inputStream = connection.getInputStream();
            return BitmapFactory.decodeStream(inputStream);  // Decode the input stream to a Bitmap
        } catch (IOException e) {
            Log.e("MainActivity", "Error downloading image", e);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                Log.e("MainActivity", "Error closing input stream", e);
            }
        }
    }

    // Helper method to save the image to storage and return the file path
    private String saveImageToStorage(Bitmap bitmap, String fileName) {
        // Get the directory for the app's private pictures directory
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Movies");
        // Ensure the directory exists
        if (!directory.exists()) {
            boolean dirCreated = directory.mkdirs();  // Create the directory and any missing parent directories
            if (!dirCreated) {
                Log.e("MainActivity", "Error creating directory for storing images.");
                return null;  // Return null if the directory couldn't be created
            }
        }
        // Create the image file
        File imageFile = new File(directory, fileName);
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);  // Save the image with 90% quality
        } catch (IOException e) {
            Log.e("MainActivity", "Error saving image to storage", e);
            return null;  // Return null if saving the file fails
        }
        return imageFile.getAbsolutePath();  // Return the file path to be stored in the database
    }

    // Method to update movie details (title, overview, category, and ratings)
    private void updateMovieDetails(Movie movie) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setMovieTitle(String.format("%s (%s)", movie.getTitle(), movie.getReleaseDateYear()));
        }
        setMovieOverview(movie.getOverview());
        setMovieCategory(movie.getCategory());
        setMovieRatings(movie.getVoteAverage());  // Pass the movie's rating

        // New fields
        setAdultStatus(movie.getAdult());  // Display adult status
        setOriginalLanguage(movie.getOriginalLanguage());  // Display original language
        setGenres(movie.getGenres());  // Display genres
        setPopularity(movie.getPopularity());  // Display popularity
        setVoteCount(movie.getVoteCount());  // Display vote count
    }
    // Methods to handle the new fields
    private void setAdultStatus(Boolean isAdult) {
        adultStatusTextView.setText(isAdult ? "Adult" : "Family-friendly");
    }

    private void setOriginalLanguage(String language) {
        originalLanguageTextView.setText(language != null ? language : "Unknown Language");
    }

    private void setGenres(String genres) {
        genresTextView.setText(genres);  // Display genre names
    }

    private void setPopularity(double popularity) {
        popularityTextView.setText(String.format("Popularity: %.2f", popularity));
    }

    private void setVoteCount(int voteCount) {
        voteCountTextView.setText(String.format("Vote Count: %d", voteCount));
    }
    // Method to set movie ratings and display the corresponding tomato icon
    private void setMovieRatings(float tomatoRating) {
        // Ensure tomatoIcon is initialized
        if (tomatoIcon == null) {
            Log.e("MainActivity", "Tomato icon ImageView is not initialized");
            return;
        }
        float scaledRating = tomatoRating * 10;
        // Set the image dynamically based on the rating
        if (scaledRating >= 60.0) {
            // Use the fresh tomato image for ratings >= 60
            tomatoIcon.setImageResource(R.drawable.fresh_tomato);
        } else {
            // Use the rotten tomato image for ratings < 60
            tomatoIcon.setImageResource(R.drawable.rotten_tomato);
        }
        String formattedRating = String.format("%.1f", scaledRating);
        // Optionally, display the numeric rating
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

    public void showNowPlaying(String title, String year, String posterUrl, String overview) {
        stopSlideshow();
        showingNowPlaying = true;  // Set the flag to indicate Now Playing is active
        Intent intent = new Intent(this, NowPlayingActivity.class);
        // Pass movie details to NowPlayingActivity
        intent.putExtra("title", title);
        intent.putExtra("overview", overview);
        intent.putExtra("year", (year.equals("0") || year.isEmpty()) ? "Unknown Year" : year);
        intent.putExtra("poster_url", posterUrl);
        // Start NowPlayingActivity
        startActivity(intent);
    }

    // Method to resume the slideshow
    public void resumeSlideshow() {
        if (!showingNowPlaying) {
            startSlideshow();
            Toast.makeText(MainActivity.this, "Posters resumed ", Toast.LENGTH_SHORT).show();
        }
    }
    public boolean isSlideshowPlaying() {
        // Check if the slideshow is currently playing
        return slideshowRunning; // Boolean variable to track the state
    }
    // Method to stop the slideshow
    public void stopSlideshow() {
        if (slideshowTimer != null) {
            slideshowTimer.cancel();
            slideshowTimer = null;
            slideshowRunning = false;
            Toast.makeText(MainActivity.this, "Posters Stopped ", Toast.LENGTH_SHORT).show();
        }
    }
    // Method to go back a slide
    private void goBackSlide() {
        if (currentImageIndex > 0) {
            currentImageIndex--;  // Move to the previous slide
        } else {
            currentImageIndex = flaskMovieList.size() - 1;  // Wrap around to the last slide if at the start
        }
        // Load the previous slide
        Movie currentMovie = flaskMovieList.get(currentImageIndex);
        loadPoster(currentMovie);
        updateMovieDetails(currentMovie);
    }
    // Method to go to the next slide
    private void goNextSlide() {
        if (currentImageIndex < flaskMovieList.size() - 1) {
            currentImageIndex++;  // Move to the next slide
        } else {
            currentImageIndex = 0;  // Wrap around to the first slide if at the end
        }
        // Load the next slide
        Movie currentMovie = flaskMovieList.get(currentImageIndex);
        loadPoster(currentMovie);
        updateMovieDetails(currentMovie);
    }
    public void setShowingNowPlaying(boolean state) {
        showingNowPlaying = state;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                 stopSlideshow();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                if(isSlideshowPlaying()) {
                    stopSlideshow();
                }else {
                    resumeSlideshow();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:  // Detect left D-pad key
                goBackSlide();  // Go to the previous slide
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:  // Detect left D-pad key
                goNextSlide();  // Go to the previous slide
                return true;
            default:
                return super.onKeyDown(keyCode, event);  // Default handling for other keys
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (slideshowTimer != null) {
            slideshowTimer.cancel();  // Stop the slideshow timer
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(nowPlayingReceiver);
    }
}
