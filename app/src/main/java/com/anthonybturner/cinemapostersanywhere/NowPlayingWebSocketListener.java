package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.MOVIE_UPDATED_INTENT_ACTION;
import static com.anthonybturner.cinemapostersanywhere.Constants.MovieConstants.PLEX_MOVIE_PLAYING_INTENT_ACTION;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.utilities.TMDBUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NowPlayingWebSocketListener {
    private final Context context;

    public NowPlayingWebSocketListener(Context context) {
        this.context = context;
    }

    public void onOpen() {
        Log.d("WebSocket", "Connected");
    }

    public void onMovieUpdate(Object data) {
        Log.d("WebSocket", "Movie update event received");
        Intent intent = new Intent(MOVIE_UPDATED_INTENT_ACTION);
        try{
            JSONObject jsonObject = new JSONObject(data.toString());
            String movieStatus = jsonObject.optString("movie_status", "{}"); // Fallback to empty JSON if missing
            intent.putExtra("movieStatus", movieStatus);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }catch (JSONException e) {
            Log.e("WebSocket", "Failed to parse JSON message", e);
        }
    }

    public void onMessage(Object data) {
        try {
            JSONObject jsonObject = new JSONObject(data.toString());
            String payload = jsonObject.optString("payload", "{}"); // Fallback to empty JSON if missing
            JSONObject payloadObject = new JSONObject(payload);

            if (payloadObject.has("Metadata")) {
                JSONObject metadata = payloadObject.getJSONObject("Metadata");

                String title = metadata.optString("title", "Unknown Title");
                String overview = metadata.optString("summary", "No overview available.");
                String contentRating = metadata.optString("contentRating", "No content rating available.");
                String studio = metadata.optString("studio", "No studio available.");
               // Bundle bundle = getActorRolesBundle(metadata);
                Bundle bundle = getMainActorRolesBundle(metadata);
                int year = metadata.optInt("year", 0);
                String tmdbId = metadata.optJSONArray("Guid").optJSONObject(1).optString("id", "tmdb://0").split("tmdb://")[1];
                TMDBUtils.fetchPosterUrlFromTMDB(context, tmdbId, posterUrl -> {
                    if (posterUrl != null) {
                        String eventType = payloadObject.optString("event", "");
                        Intent intent = new Intent(PLEX_MOVIE_PLAYING_INTENT_ACTION);
                        intent.putExtra("type", "plex");
                        intent.putExtra("id", -1);
                        intent.putExtra("title", title);
                        intent.putExtra("overview", overview);
                        intent.putExtra("contentRating", contentRating);
                        intent.putExtra("studio", studio);
                        intent.putExtra("year", year > 0 ? String.valueOf(year) : "Unknown Year");
                        intent.putExtra("posterUrl", posterUrl);
                        intent.putExtra("actorBundle", bundle);
                       // intent.putExtra("actorNotMainRoleBundle", bundle);
                        intent.putExtra("action", eventType.equals("media.resume") || eventType.equals("media.play") ? "now_playing" : "resume_slideshow");
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    } else {
                        Log.e("WebSocket", "Failed to fetch poster URL for movie: " + title);
                    }
                });
            }
        } catch (JSONException e) {
            Log.e("WebSocket", "Failed to parse JSON message", e);
        }
    }

    private static Bundle getMainActorRolesBundle(JSONObject metadata) throws JSONException {
        JSONArray roles = metadata.getJSONArray("Role");
        Bundle bundle = new Bundle(); // Create a Bundle to store data
        int maxActors = 10;
        for (int i = 0; i < roles.length(); i++) {
            if(i > maxActors){ break;}

            JSONObject roleObject = roles.getJSONObject(i);
            // Extract the actor's name, role, and image URL
            String actorName = "";
            actorName += roleObject.optString("tag", "Unknown Actor");
            String characterRole = roleObject.optString("role", "Unknown Character");
            String actorImageUrl = roleObject.optString("thumb", "");
            // Add the data to the Bundle using unique keys based on the actor's name
            bundle.putString(actorName + "_role", characterRole);
            bundle.putString(actorName + "_thumb", actorImageUrl);

        }
        return bundle;
    }
    private static Bundle getActorRolesBundle(JSONObject metadata) throws JSONException {
        JSONArray roles = metadata.getJSONArray("Role");
        Bundle bundle = new Bundle(); // Create a Bundle to store data
        // Loop through roles to populate the Bundle
        // Loop through roles to populate the Bundle in original order
        for (int i = 10; i < roles.length(); i++) {
            JSONObject roleObject = roles.getJSONObject(i);
            // Extract the actor's name, role, and image URL
            String actorName = "";
            if(i > 10){
                actorName += roleObject.optString("tag", "Unknown Actor");
                String characterRole = roleObject.optString("role", "Unknown Character");
                String actorImageUrl = roleObject.optString("thumb", "");

                // Add the data to the Bundle using unique keys based on the actor's name
                bundle.putString(actorName + "_role", characterRole);
                bundle.putString(actorName + "_thumb", actorImageUrl);
            }
        }
        return bundle;
    }

    public void onClose() {
        Log.d("WebSocket", "Disconnected");
    }
}
