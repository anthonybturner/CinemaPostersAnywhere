package com.anthonybturner.cinemapostersanywhere.services;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
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
import com.anthonybturner.cinemapostersanywhere.SteamGameActivity;
import com.anthonybturner.cinemapostersanywhere.R;
import com.anthonybturner.cinemapostersanywhere.Constants.Steam;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class SteamGameService extends Service {
    private static final String TAG = "SteamGameService";
    private static final long UPDATE_INTERVAL = 30000; // 30 seconds
    public static final String TIMER_UPDATE_ACTION = "com.anthonybturner.cinemapostersanywhere.TIMER_UPDATE";

    private Handler handler;
    private Runnable runnable;
    private RequestQueue requestQueue;
    private static String steamID;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public SteamGameService getService() {
            return SteamGameService.this;
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    public static void setSteamId(String newSteamId) {
        steamID = newSteamId;
        // Optionally, perform any additional logic needed when the Steam ID changes
        Log.d("SteamGameService", "Steam ID set to: " + steamID);
    }

    public static String getSteamId() {
        return steamID;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        requestQueue = Volley.newRequestQueue(this);
        createNotificationChannel();
        monitorActiveGameStatus();
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
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        // Retrieve the saved Steam ID
        setSteamId(sharedPreferences.getString("steam_id", null));
        return START_STICKY;
    }

    private void monitorActiveGameStatus() {
        runnable = new Runnable() {
            @Override
            public void run() {
                checkCurrentGame();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.post(runnable);
    }


    public void checkCurrentGame(){
        checkCurrentGame(false);
    }

    private boolean isPlayingApexLegends(String gameId){
        return gameId.equals("1172470");//Apex legend game id, todo: Create constant.
    }
    public void checkCurrentGame(boolean isMapOver) {
        String url = "https://api.steampowered.com/ISteamUser/GetPlayerSummaries/v2/?key=" + Steam.STEAM_API_KEY + "&steamids=" + getSteamId();
        Intent intent = new Intent(Steam.STEAM_GAME_PLAYING_ACTION);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray players = response.getJSONObject("response").getJSONArray("players");
                            if (players.length() > 0) {
                                JSONObject player = players.getJSONObject(0);
                                if (player.has("gameid")) {//Is playing steam game
                                    String gameId = player.getString("gameid");
                                    if (!MovieActivity.isSameGameDisplayed((gameId)) || isMapOver) {//Ensure the same game is not currently running
                                        //TODO: save gameId to database table(eg.Games), and fetch gameId from database on upcoming calls to reduce api request calls.
                                        // Initialize CountDownLatch to wait for both async operations
                                        intent.putExtra("gameId", player.getString("gameid"));
                                        intent.putExtra("personaname", player.getString("personaname"));
                                        intent.putExtra("avatarfull", player.getString("avatarfull"));
                                        intent.putExtra("realname", player.getString("realname"));
                                        intent.putExtra("isPlayingApexLegends", isPlayingApexLegends(gameId));

                                        CountDownLatch latch = new CountDownLatch(3);
                                        fetchGameDetails(gameId, intent, latch);
                                        fetchGameStatsAndAchievements(gameId, intent, latch);
                                        // Wait for both tasks to finish
                                        new Thread(() -> {
                                            try {
                                                latch.await();
                                                // Broadcast after both async calls complete
                                                LocalBroadcastManager.getInstance(SteamGameService.this).sendBroadcast(intent);
                                                sendTimerFinishedNotification(intent);
                                            } catch (InterruptedException e) {
                                                Log.e(TAG, "Error by latch await interruption: " + e.getMessage());
                                            }
                                        }).start();
                                    }
                                } else {
                                    // No game currently being played
                                    if (SteamGameActivity.isActive) {
                                        // SteamGameActivity is currently active
                                        stopShowingGameInfo();
                                    }
                                    Log.d(TAG, Steam.NO_GAME_CURRENTLY_BEING_PLAYED);
                                }
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error fetching json response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching current game: " + error.getMessage());
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }


    private void stopShowingGameInfo() {
        Intent intent = new Intent(Steam.STEAM_GAME_PLAYING_ACTION);
        // Send broadcast to update the UI
        intent.putExtra("game_id", ""); // Include the game ID
        intent.putExtra("game_status", "failed"); // Include the game ID
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    private void fetchGameStatsAndAchievements(String gameId, Intent intent, CountDownLatch latch) {
        // API to fetch user stats and achievements for a particular game
        String url = "https://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v2/?key=" + Steam.STEAM_API_KEY + "&steamid=" + steamID + "&appid=" + gameId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Parse user stats and achievements
                            JSONObject playerStats = response.getJSONObject("playerstats");
                            if (playerStats.has("achievements")) {
                                JSONArray achievements = playerStats.getJSONArray("achievements");
                                // Store achievements locally to fetch icons later
                                Map<String, Boolean> userAchievements = new HashMap<>();
                                for (int i = 0; i < achievements.length(); i++) {
                                    JSONObject achievement = achievements.getJSONObject(i);
                                    String name = achievement.getString("name");
                                    boolean achieved = achievement.getInt("achieved") == 1;
                                    userAchievements.put(name, achieved);
                                }
                                // Fetch achievement details including icons
                                fetchAchievementSchema(gameId, userAchievements, intent, latch);
                            }else{
                                latch.countDown();//Countdown for fetchAchievementSchema
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error fetching achievements: " + e.getMessage());
                        } finally {
                            // Signal that this task is done
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching achievements: " + error.getMessage());
                        latch.countDown(); // Ensure latch decreases even on error
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchAchievementSchema(String gameId, Map<String, Boolean> userAchievements, Intent intent, CountDownLatch latch) {
        // Fetch achievement schema to get icons
        String url = "https://api.steampowered.com/ISteamUserStats/GetSchemaForGame/v2/?key=" + Steam.STEAM_API_KEY + "&appid=" + gameId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject gameData = response.getJSONObject("game");
                            JSONObject gameStats = gameData.getJSONObject("availableGameStats");
                            JSONArray achievementsArray = gameStats.getJSONArray("achievements");

                            // Create lists to hold achievement details
                            JSONArray achievementNames = new JSONArray();
                            JSONArray achievementStatuses = new JSONArray();
                            JSONArray achievementDesc = new JSONArray();
                            JSONArray achievementIcons = new JSONArray();

                            for (int i = 0; i < achievementsArray.length(); i++) {
                                JSONObject achievement = achievementsArray.getJSONObject(i);
                                String name = achievement.getString("name");
                                boolean achieved = false;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    achieved = userAchievements.getOrDefault(name, false);
                                }
                                achievementStatuses.put(achieved ? "Unlocked" : "Locked");

                                String displayName = achievement.getString("displayName");
                                achievementNames.put(displayName);
                                // Check for the presence of a description and handle empty cases
                                if (achievement.has("description")) {
                                    String description = achievement.getString("description");
                                    if (!description.isEmpty()) {
                                        achievementDesc.put(description);
                                    } else {
                                        // Optionally add a default message for empty descriptions
                                        achievementDesc.put("No description available");
                                    }
                                } else {
                                    // If there is no "description" key at all
                                    achievementDesc.put("No description available");
                                }
                                String iconUrl = achievement.getString("icon");
                                achievementIcons.put(iconUrl);

                                // Put lists into the Intent
                                intent.putExtra("achievement_names", achievementNames.toString());
                                intent.putExtra("achievement_statuses", achievementStatuses.toString());
                                intent.putExtra("achievement_icons", achievementIcons.toString());
                                intent.putExtra("achievement_desc", achievementDesc.toString());

                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error fetching achievement schema: " + e.getMessage());
                        }finally {
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching achievement schema: " + error.getMessage());
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchGlobalAchievementPercentages(String gameId, Map<String, Boolean> userAchievements, Intent intent, CountDownLatch latch) {
        // Fetch global achievement percentages
        String url = "https://api.steampowered.com/ISteamUserStats/GetGlobalAchievementPercentagesForApp/v2/?key=" + Steam.STEAM_API_KEY + "&gameid=" + gameId;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Access the "achievementpercentages" object in the response
                            JSONObject achievementPercentages = response.getJSONObject("achievementpercentages");
                            JSONArray achievementsArray = achievementPercentages.getJSONArray("achievements");

                            // Create lists to hold achievement details
                            JSONArray achievementNames = new JSONArray();
                            JSONArray achievementStatuses = new JSONArray();
                            JSONArray achievementPercentArray = new JSONArray();

                            for (int i = 0; i < achievementsArray.length(); i++) {
                                JSONObject achievement = achievementsArray.getJSONObject(i);
                                String name = achievement.getString("name");
                                double globalPercentage = achievement.getDouble("percent");

                                // Add the global achievement percentage to the list
                                achievementPercentArray.put(globalPercentage);

                                boolean achieved = false;
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    achieved = userAchievements.getOrDefault(name, false);
                                }
                                achievementStatuses.put(achieved ? "Unlocked" : "Locked");
                                achievementNames.put(name);  // Use the internal name of the achievement
                                // Put lists into the Intent
                                intent.putExtra("achievement_names", achievementNames.toString());
                                intent.putExtra("achievement_statuses", achievementStatuses.toString());
                                intent.putExtra("achievement_percentages", achievementPercentArray.toString());
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Error parsing global achievement percentages: " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching global achievement percentages: " + error.getMessage());
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void fetchGameDetails(String gameId, Intent intent, CountDownLatch latch) {
        String url = String.format("https://store.steampowered.com/api/appdetails?appids=%s&l=english", gameId);
        Context context = this;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject gameData = response.getJSONObject(gameId).getJSONObject("data");
                            String gameName = gameData.getString("name");
                            String description = gameData.getString("short_description");
                            String releaseDate = gameData.getJSONObject("release_date").getString("date");
                            String posterImageUrl = gameData.getString("header_image"); // Get the poster image URL
                            // Extract system requirements
                            String minimumRequirements = "";
                            String recommendedRequirements = "";
                            if (gameData.has("pc_requirements")) {
                                JSONObject pcRequirements = gameData.getJSONObject("pc_requirements");
                                if (pcRequirements.has("minimum")) {
                                    minimumRequirements = pcRequirements.getString("minimum");
                                }
                                if (pcRequirements.has("recommended")) {
                                    recommendedRequirements = pcRequirements.getString("recommended");
                                }
                            }
                            // Send broadcast to update the UI
                            intent.putExtra("game_id", gameId); // Include the game ID
                            intent.putExtra("game_name", gameName);
                            intent.putExtra("description", description);
                            intent.putExtra("release_date", releaseDate);
                            intent.putExtra("poster_image_url", posterImageUrl); // Add poster image URL
                            intent.putExtra("minimum_requirements", minimumRequirements); // Add minimum system requirements
                            intent.putExtra("recommended_requirements", recommendedRequirements); // Add recommended system requirements
                            intent.putExtra("game_status", "success"); // Include the game status
                        } catch (JSONException e) {
                            Log.e(TAG, "Error fetching JSON details: " + e.getMessage());
                        }finally {
                            // Signal that this task is done
                            latch.countDown();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error fetching game details: " + error.getMessage());
                        latch.countDown();
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }


}
