package com.anthonybturner.cinemapostersanywhere;

import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.MOVIE_UPDATED_INTENT_ACTION;
import static com.anthonybturner.cinemapostersanywhere.utilities.Constants.NOW_PLAYING_INTENT_ACTION;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.anthonybturner.cinemapostersanywhere.utilities.TMDBUtils;

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
                int year = metadata.optInt("year", 0);
                String tmdbId = metadata.optJSONArray("Guid").optJSONObject(1).optString("id", "tmdb://0").split("tmdb://")[1];

                TMDBUtils.fetchPosterUrlFromTMDB(context, tmdbId, posterUrl -> {
                    if (posterUrl != null) {
                        String eventType = payloadObject.optString("event", "");
                        Intent intent = new Intent(NOW_PLAYING_INTENT_ACTION);
                        intent.putExtra("title", title);
                        intent.putExtra("overview", overview);
                        intent.putExtra("year", year > 0 ? String.valueOf(year) : "Unknown Year");
                        intent.putExtra("posterUrl", posterUrl);
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

    public void onClose() {
        Log.d("WebSocket", "Disconnected");
    }
}
