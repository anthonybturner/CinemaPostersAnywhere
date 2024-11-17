package com.anthonybturner.cinemapostersanywhere.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.anthonybturner.cinemapostersanywhere.MovieActivity;
import com.anthonybturner.cinemapostersanywhere.interfaces.TimerUpdateListener;
import com.anthonybturner.cinemapostersanywhere.R;
import com.anthonybturner.cinemapostersanywhere.utilities.Converters;
import com.anthonybturner.cinemapostersanywhere.utilities.MovieConstants;
import com.anthonybturner.cinemapostersanywhere.utilities.TMDBUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MovieService extends Service {
    private static final String TAG = "MovieService";
    public static final String STEAM_API_KEY = "2340AF30162B10A27AA52B1E3002275D";
    private static final String APEX_API_KEY = "e95f0a39efc186fe2aaa0e4d19f2b65c";
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds
    public static final String NO_GAME_CURRENTLY_BEING_PLAYED = "No game currently being played.";
    public static final String TIMER_UPDATE_ACTION = "com.anthonybturner.cinemapostersanywhere.TIMER_UPDATE";

    private Handler handler;
    private Runnable runnable;
    private RequestQueue requestQueue;
    private CountDownTimer mapCountDownTimer, rankedMapCountDownTimer, arenasMapCountDownTimer;
    private static String steamID;
    private List<TimerUpdateListener> listenersList;
    private TimerUpdateListener timerUpdateListener;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MovieService getService() {
            return MovieService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setTimerCallback(TimerUpdateListener callback) {
        listenersList.add(callback);
        //this.timerUpdateListener = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        requestQueue = Volley.newRequestQueue(this);
        listenersList = new ArrayList<TimerUpdateListener>();
        createNotificationChannel();
        monitorActiveMovieStatus();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "MovieTimerChannel";
            String description = "Channel for movie timer notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("TIMER_CHANNEL_ID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void sendTimerFinishedNotification(Intent mapIntent) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted; don't post the notification
            return;
        }
        Intent intent = new Intent(this, MovieActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String map = mapIntent.getStringExtra("next_map");
        String durationInMins = mapIntent.getStringExtra("durationInMins");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TIMER_CHANNEL_ID")
                .setSmallIcon(R.drawable.cinema) // Replace with your app’s icon
                .setContentTitle(String.format("%s started", map))
                .setContentText(String.format("Apex Legends - %s is up! %s minutes", map, durationInMins))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build()); // Use a unique ID for each notification
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Access SharedPreferences
        return START_STICKY;
    }

    private void monitorActiveMovieStatus() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkCurrentMovie();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.post(runnable);
    }
    public void checkCurrentMovie() {
        String url = "http://192.168.1.171:8080/jsonrpc";
        try {
            JSONObject jsonRequest = createGetItemRequestParams(); // Create the JSON-RPC payload
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, jsonRequest,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            handleMovieResponse(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Error fetching current movie: " + error.getMessage());
                        }
                    });
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void handleMovieResponse(JSONObject response) {
        try {
            // Early exit if "result" is not present in the response
            if (!response.has("result")) {
                checkAndResumeSlideshow();
                return; // Exit early
            }

            JSONObject results = response.getJSONObject("result");

            // Early exit if "item" is not present in the results
            if (!results.has("item")) {
                checkAndResumeSlideshow();
                return; // Exit early
            }

            JSONObject item = results.getJSONObject("item");
            if (isItemValid(item)) {
                long movieId = item.getLong("id");
                if (MovieActivity.isSameMovie(movieId)) return;
                // Populate the intent with movie details
                Intent intent = buildMovieIntent(item, movieId);
                fetchDirectorImages(item, intent);
            } else {
                checkAndResumeSlideshow();
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
        }
    }

    private void checkAndResumeSlideshow() {
        if(!MovieActivity.isSlideshowPlaying()){
            sendSlideshowResumeIntent();
        }
    }
    // Helper method to validate the item
    private boolean isItemValid(JSONObject item) throws JSONException {
        return item.has("title") && !item.getString("title").isEmpty()
                && item.has("file") && !item.getString("file").isEmpty()
                && item.has("thumbnail") && !item.getString("thumbnail").isEmpty();
    }
    private Intent buildMovieIntent(JSONObject item, long movieId) throws JSONException {
        Intent intent = new Intent(MovieConstants.KODI_MOVIE_PLAYING_INTENT_ACTION);
        intent.putExtra("action", "now_playing");
        intent.putExtra("type", "kodi");
        intent.putExtra("id", movieId);
        intent.putExtra("title", item.getString("title"));
        intent.putExtra("overview", item.getString("plot"));
        intent.putExtra("contentRating", item.getDouble("rating"));
        intent.putExtra("year", item.getString("year"));
        intent.putExtra("runtime", item.getLong("runtime"));
        intent.putExtra("posterUrl", getPosterImage(item));
        return intent;
    }

    private void fetchDirectorImages(JSONObject item, Intent intent) throws JSONException {
        JSONArray directors = item.getJSONArray("director");
        HashMap<String, String> directorList = new HashMap<>();
        final int directorCount = directors.length();
        final AtomicInteger fetchedCount = new AtomicInteger(0);

        for (int i = 0; i < directorCount; i++) {
            String director = directors.getString(i);
            TMDBUtils.fetchDirectorImage(MovieService.this, posterUrl -> {
                if (posterUrl != null) {
                    directorList.put(director, posterUrl);
                }

                // When all directors are fetched, broadcast the intent
                if (fetchedCount.incrementAndGet() == directorCount) {
                    addDirectorDataToIntent(intent, directorList);
                    LocalBroadcastManager.getInstance(MovieService.this).sendBroadcast(intent);
                    sendTimerFinishedNotification(intent);
                }
            }, director);
        }
    }

    private void addDirectorDataToIntent(Intent intent, HashMap<String, String> directorList) {
        ArrayList<String> directorNames = new ArrayList<>(directorList.keySet());
        ArrayList<String> directorImages = new ArrayList<>(directorList.values());
        intent.putStringArrayListExtra("directorNames", directorNames);
        intent.putStringArrayListExtra("directorImages", directorImages);
    }

    private void sendSlideshowResumeIntent() {
        Intent intent = new Intent(MovieConstants.KODI_MOVIE_PLAYING_INTENT_ACTION);
        intent.putExtra("action", "resume_slideshow");
        LocalBroadcastManager.getInstance(MovieService.this).sendBroadcast(intent);
    }

    private static String getPosterImage(JSONObject item) throws JSONException {
        String fanart= "";
        fanart = item.getString("fanart");
        fanart = Converters.decodeUrl(fanart);
        if (fanart.startsWith("image://")) {
            fanart = fanart.substring("image://".length());
        }
        if (fanart.endsWith("/")) {
            fanart = fanart.substring(0, fanart.length() - 1);
        }
        return fanart;
    }

    private JSONObject createGetItemRequestParams() throws JSONException {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("jsonrpc", "2.0");
        jsonRequest.put("id", 1);
        jsonRequest.put("method", "Player.GetItem");

        // Parameters for the request
        JSONObject params = new JSONObject();
        params.put("playerid", 1); // Player ID 1 is usually for videos
        params.put("properties", new JSONArray(new String[]{"title", "genre", "year", "rating", "plot", "director", "runtime", "fanart", "thumbnail", "file"}));
        jsonRequest.put("params", params);
        return jsonRequest;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}
