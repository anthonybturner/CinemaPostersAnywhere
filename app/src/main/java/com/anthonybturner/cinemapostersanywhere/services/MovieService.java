// This service is responsible for monitoring the current movie playing on Kodi and sending the movie details to the MainActivity.
package com.anthonybturner.cinemapostersanywhere.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
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
import com.anthonybturner.cinemapostersanywhere.Constants.SharedPrefsConstants;
import com.anthonybturner.cinemapostersanywhere.MainActivity;
import com.anthonybturner.cinemapostersanywhere.interfaces.FetchCompleteCallback;
import com.anthonybturner.cinemapostersanywhere.R;
import com.anthonybturner.cinemapostersanywhere.utilities.Converters;
import com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants;
import com.anthonybturner.cinemapostersanywhere.utilities.TMDBUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
public class MovieService extends Service {
    private static final String TAG = "MovieService";
    private static final long UPDATE_INTERVAL = 10000; // 10 seconds

    private Handler handler;
    private Runnable runnable;
    private RequestQueue requestQueue;
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

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        requestQueue = Volley.newRequestQueue(this);
        createNotificationChannel();
        monitorActiveMovieStatus();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
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

    private void sendTimerFinishedNotification(Intent movieIntent) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String movieTitle = movieIntent.getStringExtra("title");
        String movieOverview = movieIntent.getStringExtra("overview");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "TIMER_CHANNEL_ID")
                .setSmallIcon(R.drawable.cinema)
                .setContentTitle(String.format("%s now playing", movieTitle))
                .setContentText(String.format("Overview: %s", movieOverview))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
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
        SharedPreferences sharedPreferences = getSharedPreferences(SharedPrefsConstants.PREFS_KEY_APP_PREFERENCES, MODE_PRIVATE);
        String kodiIP = sharedPreferences.getString(SharedPrefsConstants.PREF_KEY_KODI_IP_ADDRESS, SharedPrefsConstants.PREF_VALUE_DEFAULT_KODI_IP_ADDRESS);
        String kodiPort = sharedPreferences.getString(SharedPrefsConstants.PREF_KEY_KODI_PORT, SharedPrefsConstants.PREF_VALUE_DEFAULT_KODI_PORT);
        String kodiUrl = String.format("http://%s:%s/jsonrpc", kodiIP, kodiPort);

        try {
            JSONObject jsonRequest = createGetItemRequestParams();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, kodiUrl, jsonRequest,
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
        } catch (JSONException error) {
            Log.e(TAG, "Error fetching current movie: " + error.getMessage());
        }
    }

    private void handleMovieResponse(JSONObject response) {
        boolean isResumingSlideShow = false;
        try {
            if (response.has("result") && response.getJSONObject("result").has("item")) {
                JSONObject results = response.getJSONObject("result");
                JSONObject item = results.getJSONObject("item");
                if (isItemValid(item)) {
                    long movieId = item.getLong("id");
                    if (MainActivity.isSameMovie(movieId)) return;
                    CreateMovie(item, movieId);
                } else {
                    isResumingSlideShow = true;
                }
            } else {
                isResumingSlideShow = true;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response: " + e.getMessage());
        } finally {
            if (isResumingSlideShow) {
                NotifyResumeSlideshow();
            }
        }
    }

    private void CreateMovie(JSONObject item, long movieId) throws JSONException {
        Intent intent = CreateMovieIntent(item, movieId);
        fetchDirectorImages(item, intent, movieIntent -> {
            sendTimerFinishedNotification(movieIntent);
            LocalBroadcastManager.getInstance(MovieService.this).sendBroadcast(movieIntent);
        });
    }

    private void NotifyResumeSlideshow() {
        if (!MainActivity.isSlideshowPlaying()) {
            Intent intent = new Intent(MovieConstants.ACTION_MOVIE_RESUME_SLIDESHOW);
            LocalBroadcastManager.getInstance(MovieService.this).sendBroadcast(intent);
        }
    }

    private boolean isItemValid(JSONObject item) throws JSONException {
        return item.has("title") && !item.getString("title").isEmpty()
                && item.has("file") && !item.getString("file").isEmpty()
                && item.has("thumbnail") && !item.getString("thumbnail").isEmpty();
    }

    @NonNull
    private Intent CreateMovieIntent(JSONObject item, long movieId) throws JSONException {
        Intent intent = new Intent(MovieConstants.ACTION_KODI_MOVIE_PLAYING);
        intent.putExtra("action", MovieConstants.ACTION_MOVIE_NOW_PLAYING);
        intent.putExtra("category", "Now Playing (Kodi)");
        intent.putExtra("type", "kodi");
        intent.putExtra("id", movieId);
        intent.putExtra("title", item.getString("title"));
        intent.putExtra("overview", item.getString("plot"));
        intent.putExtra("contentRating", item.getDouble("rating"));
        intent.putExtra("year", item.getString("year"));
        intent.putExtra("runtime", item.getLong("runtime"));
        intent.putExtra("posterUrl", getPosterImage(item));

// Adding new parameters
        intent.putExtra("originalTitle", item.optString("originaltitle", ""));
        intent.putExtra("cast", getCastList(item));  // A method to extract and format the cast list
        intent.putExtra("writer", item.optString("writer", ""));
        intent.putExtra("studio", item.optString("studio", ""));
        intent.putExtra("tagline", item.optString("tagline", ""));
        intent.putExtra("country", item.optString("country", ""));
        intent.putExtra("imdbNumber", item.optString("imdbnumber", ""));
        intent.putExtra("mpaa", item.optString("mpaa", "Uknown"));
        intent.putExtra("trailer", item.optString("trailer", ""));
        intent.putExtra("top250", item.optInt("top250", -1));  // -1 if not available
        intent.putExtra("set", item.optString("set", ""));
        intent.putExtra("dateAdded", item.optString("dateadded", ""));
        intent.putExtra("votes", item.optInt("votes", 0));  // Default to 0 if not available
        intent.putExtra("lastPlayed", item.optString("lastplayed", ""));
        intent.putExtra("tags", getTagsList(item));  // A method to extract tags, if available
        intent.putExtra("art", getArt(item));  // A method to extract artwork links

// Adding genre
        JSONArray genreArray = item.getJSONArray("genre");
        ArrayList<String> genreList = new ArrayList<>();
        for (int i = 0; i < genreArray.length(); i++) {
            genreList.add(genreArray.getString(i));
        }
        intent.putStringArrayListExtra("genre", genreList);

        return intent;
    }

    private ArrayList<String> getCastList(JSONObject item) {
        ArrayList<String> castList = new ArrayList<>();
        try {
            JSONArray castArray = item.getJSONArray("cast");
            for (int i = 0; i < castArray.length(); i++) {
                JSONObject castMember = castArray.getJSONObject(i);
                castList.add(castMember.getString("name") + " (" + castMember.optString("role", "N/A") + ")");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return castList;
    }

    private ArrayList<String> getTagsList(JSONObject item) {
        ArrayList<String> tagsList = new ArrayList<>();
        try {
            JSONArray tagsArray = item.getJSONArray("tag");
            for (int i = 0; i < tagsArray.length(); i++) {
                tagsList.add(tagsArray.getString(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return tagsList;
    }

    private String getArt(JSONObject item) {
        try {
            JSONObject art = item.getJSONObject("art");
            return art.optString("poster", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    private void fetchDirectorImages(JSONObject item, Intent intent, FetchCompleteCallback callback) throws JSONException {
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
                if (fetchedCount.incrementAndGet() == directorCount) {
                    addDirectorDataToIntent(intent, directorList);
                    callback.onFetchComplete(intent);
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

    private static String getPosterImage(JSONObject item) throws JSONException {
        String fanart = item.getString("fanart");
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

        JSONObject params = new JSONObject();
        params.put("playerid", 1);
        params.put("properties", new JSONArray(new String[]{
                "title", "genre", "year", "rating", "plot", "director", "runtime", "fanart",
                "thumbnail", "file", "originaltitle", "cast", "writer", "studio", "tagline",
                "country", "imdbnumber", "mpaa", "trailer", "top250", "set", "dateadded",
                "votes", "lastplayed", "tag", "art"
        }));
        jsonRequest.put("params", params);
        return jsonRequest;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}