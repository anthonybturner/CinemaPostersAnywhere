package com.anthonybturner.cinemapostersanywhere.utilities;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONException;

public class TMDBUtils {

    public static final String TMDB_API_KEY = "b59c4a80099a77209b69326bb1f66c8c";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original";

    public interface PosterCallback {
        void onPosterUrlFetched(String posterUrl);
    }

    public static void fetchPosterUrlFromTMDB(Context context, String tmdbId, PosterCallback callback) {
        String url = "https://api.themoviedb.org/3/movie/" + tmdbId + "?api_key=" + TMDB_API_KEY;
        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Get the poster path and return it via callback
                        String posterPath = response.getString("poster_path");
                        String posterUrl = IMAGE_BASE_URL + posterPath;
                        callback.onPosterUrlFetched(posterUrl);
                    } catch (JSONException e) {
                        Log.d("TMDBUtils", "JSON Response failed", e);
                        callback.onPosterUrlFetched(null);
                    }
                },
                e -> {
                    Log.d("TMDBUtils", "JSON error Response", e);
                    callback.onPosterUrlFetched(null);
                });

        queue.add(request);
    }

    public static void fetchPosterFromTMDB(Activity activity, String tmdbId, ImageView posterImageView) {
        String url = "https://api.themoviedb.org/3/movie/" + tmdbId + "?api_key=" + TMDB_API_KEY;

        RequestQueue queue = Volley.newRequestQueue(activity);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        // Get the poster path and load the image using Glide
                        String posterPath = response.getString("poster_path");
                        String posterUrl = IMAGE_BASE_URL + posterPath;

                        Glide.with(activity)
                                .load(posterUrl)
                                .into(posterImageView);

                    } catch (JSONException e) {
                        Log.d("TMDBUtils", "JSON Response failed fetching poster from TMDB", e);
                    }
                },
                e -> Log.d("TMDBUtils", "JSON error response failed fetching poster from TMDB", e));
        queue.add(request);
    }
}
