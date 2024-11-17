package com.anthonybturner.cinemapostersanywhere.utilities;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.ImageView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TMDBUtils {

    public static final String TMDB_API_KEY = "b59c4a80099a77209b69326bb1f66c8c";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/original";
    private static final String PERSON_BASE_URL = "https://api.themoviedb.org/3/search/person?api_key=" + TMDB_API_KEY + "&query=";

    public interface PosterCallback {
        void onPosterUrlFetched(String posterUrl);
    }
    public interface ImageFetchCallback {
        void onImageFetched(String posterUrl);
    }
    public static void fetchDirectorImage(Context context, ImageFetchCallback callback, String directorName) {
        String url = PERSON_BASE_URL + directorName;
        RequestQueue queue = Volley.newRequestQueue(context);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray results = response.getJSONArray("results");
                            if (results.length() > 0) {
                                JSONObject director = results.getJSONObject(0);
                                String profilePath = director.optString("profile_path", null);
                                if (profilePath != null) {
                                    String imageUrl = "https://image.tmdb.org/t/p/w500" + profilePath;
                                    callback.onImageFetched(imageUrl);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            callback.onImageFetched(null);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        callback.onImageFetched(null);
                    }
                });

        queue.add(jsonObjectRequest);
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
