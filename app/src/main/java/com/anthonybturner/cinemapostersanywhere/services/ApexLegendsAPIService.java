package com.anthonybturner.cinemapostersanywhere.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.anthonybturner.cinemapostersanywhere.interfaces.TimerUpdateListener;
import com.anthonybturner.cinemapostersanywhere.Constants.ApexLegendsAPIConstants;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ApexLegendsAPIService extends Service {
    private static final String TAG = "APEX_LEGEND_API_SERVICE";
    private Handler handler;
    private RequestQueue requestQueue;
    private CountDownTimer publicMapCountDownTimer, rankedMapCountDownTimer, arenasMapCountDownTimer, rankedStatsCountDownTimer;
    private Runnable runnable;
    private List<TimerUpdateListener> listenersList;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public ApexLegendsAPIService getService() {
            return ApexLegendsAPIService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setTimerCallback(TimerUpdateListener callback) {
        listenersList.add(callback);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        requestQueue = Volley.newRequestQueue(this);
        listenersList = new ArrayList<TimerUpdateListener>();
        createNotificationChannel();
        checkModesMapRotations();
        checkPlayerStats();
    }

    private void checkPlayerStats() {

        CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(ApexLegendsAPIConstants.APEX_LEGENDS_API_PLAYER_STATS);
        GetPlayerRankedStats(intent, latch);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TimerChannel";
            String description = "Channel for timer notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("TIMER_CHANNEL_ID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Access SharedPreferences
        return START_STICKY;
    }

    public void checkModesMapRotations() {
        CountDownLatch latch = new CountDownLatch(3);
        Intent intent = new Intent(ApexLegendsAPIConstants.APEX_LEGENDS_API_MAP_ROTATION);
        GetPubsMapRotations(intent, latch);
        GetRankedMapRotations(intent, latch);
    }

    public void startRankedMapTimer(Intent intent, long intervalInMillis) {
        // Cancel any existing ranked timer to avoid overlapping
        if (rankedMapCountDownTimer != null) {
            rankedMapCountDownTimer.cancel();
        }
        for (TimerUpdateListener listener : listenersList) {
            if (listener != null) {
                listener.onTimerStarted(intent, true, false);
            }
        }
        // Initialize the ranked timer
        rankedMapCountDownTimer = new CountDownTimer(intervalInMillis, 1000) { // 1-second interval
            @Override
            public void onTick(long millisUntilFinished) {
                for (TimerUpdateListener listener : listenersList) {
                    if (listener != null) {
                        listener.onTimerUpdate(millisUntilFinished, true, false);
                    }
                }
            }
            @Override
            public void onFinish() {
                for (TimerUpdateListener listener : listenersList) {
                    if (listener != null) {
                        listener.onTimerFinish(true);
                    }
                }
                checkRankedMapRotations();
            }
        }.start();
    }

    private void checkRankedMapRotations() {
        CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(ApexLegendsAPIConstants.APEX_LEGENDS_API_MAP_ROTATION);
        GetRankedMapRotations(intent, latch);
    }

    public void startPublicMapTimer(Intent intent, long intervalInMillis) {
        if (publicMapCountDownTimer != null) {
            publicMapCountDownTimer.cancel();
        }
        for (TimerUpdateListener listener : listenersList) {
            if (listener != null) {
                listener.onTimerStarted(intent, false, false);
            }
        }
        publicMapCountDownTimer = new CountDownTimer(intervalInMillis, 1000) { // 1 second interval
            @Override
            public void onTick(long millisUntilFinished) {
                for (TimerUpdateListener listener : listenersList) {
                    if (listener != null) {
                        listener.onTimerUpdate(millisUntilFinished, false, false);
                    }
                }
            }

            @Override
            public void onFinish() {
                for (TimerUpdateListener listener : listenersList) {
                    if (listener != null) {
                        listener.onTimerFinish(false);
                    }
                }
                checkPublicMapRotations();
            }
        }.start();
    }

    private void checkPublicMapRotations() {
        CountDownLatch latch = new CountDownLatch(1);
        Intent intent = new Intent(ApexLegendsAPIConstants.APEX_LEGENDS_API_MAP_ROTATION);
        GetPubsMapRotations(intent, latch);
    }

    public void stopPublicMapTimer() {
        publicMapCountDownTimer.cancel();
    }

    private void GetPubsMapRotations(Intent intent, CountDownLatch latch) {
        String url = String.format("https://api.mozambiquehe.re/maprotation?auth=%s", ApexLegendsAPIConstants.APEX_API_KEY);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // For Public Maps
                            long milliseconds = parseMapRotation(response, intent,
                                    null, "current", "next",
                                    "battle_royale_map", "battle_royale_asset", "battle_royale_remainingSecs",
                                    "next_battle_royale_map", "next_battle_royale_readableDate_start",
                                    "battle_royale_next_readableDate_end", "battle_royale_next_DurationInMinutes",
                                    false, false);
                                startPublicMapTimer(intent, milliseconds);
                        } finally {
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching Apex Legends map rotation data: " + error.getMessage());
                        latch.countDown();
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void GetRankedMapRotations(Intent intent, CountDownLatch latch) {
        String url = String.format("https://api.mozambiquehe.re/maprotation?auth=%s&version=%s", ApexLegendsAPIConstants.APEX_API_KEY, "2");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // For Ranked Maps
                            long milliseconds = parseMapRotation(response, intent,
                                    "ranked", "current", "next",
                                    "ranked_map", "ranked_asset", "ranked_remainingSecs",
                                    "next_ranked_map", "next_ranked_readableDate_start",
                                    "next_ranked_readableDate_end", "next_ranked_DurationInMinutes",
                                    true, false);
                            // Set timer for either ranked or public
                            startRankedMapTimer(intent, milliseconds);
                            // For Arenas Maps
                            milliseconds = parseMapRotation(response, intent,
                                    "ltm", "current", "next",
                                    "arenas_map", "arenas_asset", "arenas_remainingSecs",
                                    "next_arenas_map", "next_arenas_readableDate_start",
                                    "next_arenas_readableDate_end", "next_arenas_DurationInMinutes",
                                    false, true);

                            // Set timer for either ranked or public
                            startArenasMapTimer(milliseconds);
                        } finally {
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching Apex Legends map rotation data: " + error.getMessage());
                        latch.countDown();
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void startArenasMapTimer(long milliseconds) {
    }


    public void parseRankedMapRotation(JSONObject response, Intent intent) {
        try {
            //Get ranked map rotation data
            JSONObject currentMode = response.getJSONObject("ranked");
            JSONObject currentMap = currentMode.getJSONObject("current");
            extractMapData(currentMap, intent, "ranked_map", "ranked_asset", "ranked_remainingSecs");
            // Parse next map rotation data
            JSONObject nextMap = currentMode.getJSONObject("next");
            extractMapData(nextMap, intent, "next_ranked_map", "next_ranked_readableDate_start", "next_ranked_readableDate_end", "next_ranked_DurationInMinutes");
            long milliSeconds = currentMap.getLong("remainingSecs") * 1000;
            startRankedMapTimer(intent, milliSeconds);
        } catch (Exception e) {
            Log.e(TAG, "Error fetching achievements: " + e.getMessage());
        }
    }

    public long parseMapRotation(JSONObject response, Intent intent, String modeKey, String currentMapKey, String nextMapKey,
                                 String mapNameKey, String assetKey, String remainingSecsKey,
                                 String nextMapNameKey, String nextStartKey, String nextEndKey,
                                 String nextDurationKey, boolean isRanked, boolean isArenas) {
        long milliSeconds = -1;
        try {
            JSONObject currentMap = null;
            JSONObject nextMap = null;
            // Get the current mode (public or ranked)
            if (modeKey == null) {
                currentMap = response.getJSONObject(currentMapKey);
                nextMap = response.getJSONObject(nextMapKey);
            } else {
                JSONObject currentMode = response.getJSONObject(modeKey);
                currentMap = currentMode.getJSONObject(currentMapKey);
                nextMap = currentMode.getJSONObject(nextMapKey);
            }
            // Parse current map rotation data
            extractMapData(currentMap, intent, mapNameKey, assetKey, remainingSecsKey);

            // Parse next map rotation data
            extractMapData(nextMap, intent, nextMapNameKey, nextStartKey, nextEndKey, nextDurationKey);
            milliSeconds = currentMap.getLong("remainingSecs") * 1000;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching map rotation data: " + e.getMessage());
        }
        return  milliSeconds;
    }

    private void GetPlayerRankedStats(Intent intent, CountDownLatch latch) {
        String url = String.format("https://api.mozambiquehe.re/bridge?auth=%s&player=%s&platform=PC", ApexLegendsAPIConstants.APEX_API_KEY, "SilentCod3r");
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // For Ranked Maps
                            parsePlayerStats(response, intent);
                            startRankedStatsTimer(intent, 60000);
                        } finally {
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching Apex Legends map rotation data: " + error.getMessage());
                        latch.countDown();
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void startRankedStatsTimer(Intent intent, int intervalInMillis) {
        // Cancel any existing ranked timer to avoid overlapping
        if (rankedStatsCountDownTimer != null) {
            rankedStatsCountDownTimer.cancel();
        }
        for (TimerUpdateListener listener : listenersList) {
            if (listener != null) {
                listener.onPlayerStatsTimerStarted(intent, true );
            }
        }
        // Initialize the ranked timer
        rankedStatsCountDownTimer = new CountDownTimer(intervalInMillis, 1000) { // 1-second interval
            @Override
            public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish() {
              // GetPlayerRankedStats(intent, latch);
            }
        }.start();
    }

    private void parsePlayerStats(JSONObject response, Intent intent) {
        try {
            JSONObject globalObject =  response.getJSONObject("global");
            intent.putExtra("name", globalObject.getString("name"));
            intent.putExtra("tag", globalObject.getString("tag"));
            intent.putExtra("avatar", globalObject.getString("avatar"));
            intent.putExtra("platform", globalObject.getString("platform"));
            intent.putExtra("level", globalObject.getString("level"));
            intent.putExtra("toNextLevelPercent", globalObject.getString("toNextLevelPercent"));
            parsePlayerRankedStats(globalObject, intent);

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private void parsePlayerRankedStats(JSONObject response, Intent intent) {
        try {
            JSONObject rankedObject =  response.getJSONObject("rank");
            intent.putExtra("rankScore", rankedObject.getString("rankScore"));
            intent.putExtra("rankName", rankedObject.getString("rankName"));
            intent.putExtra("rankDiv", rankedObject.getString("rankDiv"));
            intent.putExtra("rankImg", rankedObject.getString("rankImg"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private void extractMapData(JSONObject mapData, Intent intent, String mapKey, String assetKey, String remainingSecsKey) {
        try {
            String map = mapData.getString("map");
            String asset = mapData.getString("asset");
            // Put extracted data into intent
            intent.putExtra(mapKey, map);
            intent.putExtra(assetKey, asset);
            // Check if remainingSecsKey is present
            if (mapData.has("remainingSecs")) {
                long remainingSecs = mapData.getLong("remainingSecs");
                intent.putExtra(remainingSecsKey, remainingSecs);
            } else {
                intent.putExtra(remainingSecsKey, 0); // Default value if not present
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting map data: " + e.getMessage());
        }
    }

    private void extractMapData(JSONObject mapData, Intent intent, String mapKey, String readableDateStartKey, String readableDateEndKey, String durationMinutesKey) {
        try {
            String map = mapData.getString("map");
            String readableDateStart = mapData.getString("readableDate_start");
            String readableDateEnd = mapData.getString("readableDate_end");
            String durationMinutes = mapData.getString("DurationInMinutes");

            // Put extracted data into intent
            intent.putExtra(mapKey, map);
            intent.putExtra(readableDateStartKey, readableDateStart);
            intent.putExtra(readableDateEndKey, readableDateEnd);
            intent.putExtra(durationMinutesKey, durationMinutes);

        } catch (Exception e) {
            Log.e(TAG, "Error extracting next map data: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        if (publicMapCountDownTimer != null) publicMapCountDownTimer.cancel();
        if (rankedMapCountDownTimer != null) rankedMapCountDownTimer.cancel();
        if (arenasMapCountDownTimer != null) arenasMapCountDownTimer.cancel();
        if (rankedStatsCountDownTimer != null) rankedStatsCountDownTimer.cancel();

    }
}
