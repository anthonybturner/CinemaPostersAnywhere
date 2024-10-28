package com.anthonybturner.cinemapostersanywhere;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anthonybturner.cinemapostersanywhere.services.SteamGameService;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsActivity extends AppCompatActivity {

    private EditText steamNameEditText;
    private Button saveButton;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private static final String STEAM_NAME_KEY = "steam_name";
    private TextView steamIdTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        steamNameEditText = findViewById(R.id.steamNameEditText);
        saveButton = findViewById(R.id.saveButton);
        steamIdTextView = findViewById(R.id.steamId); // Assuming you have a TextView for Steam ID
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        // Initialize the listener
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals("steam_id")) {
                    // Handle the change
                    String newSteamID = sharedPreferences.getString(key, null);
                    Log.d("SettingsActivity", "New Steam ID: " + newSteamID);
                    // Update UI or perform any action needed
                    SteamGameService.setSteamId(newSteamID);
                }
            }
        };
        loadSteamName();
        loadSteamID(); // Load Steam ID when activity starts

        saveButton.setOnClickListener(v -> {
            String steamName = steamNameEditText.getText().toString();
            if (!steamName.isEmpty()) {
                saveSteamName(steamName);
                fetchSteamID(steamName);
            } else {
                Toast.makeText(SettingsActivity.this, "Please enter a Steam name", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void loadSteamName() {
        // Retrieve the saved Steam name
        String savedSteamName = sharedPreferences.getString(STEAM_NAME_KEY, null);
        if (savedSteamName != null) {
            steamNameEditText.setText(savedSteamName); // Set the EditText to the saved Steam name
        }
    }

    private void saveSteamName(String steamName) {
        // Save the Steam name in SharedPreferences
        sharedPreferences.edit().putString(STEAM_NAME_KEY, steamName).apply();
        Toast.makeText(this, "Steam name saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadSteamID() {
        // Retrieve the saved Steam ID
        String steamID = sharedPreferences.getString("steam_id", null);
        if (steamID != null) {
            steamIdTextView.setText(steamID); // Update the UI to show the Steam ID
        }
    }

    private void fetchSteamID(String steamName) {
        new FetchSteamIDTask().execute(steamName);
    }

    private class FetchSteamIDTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            String steamName = params[0];
            String apiUrl = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=" + SteamGameService.STEAM_API_KEY + "&vanityurl=" + steamName;

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Handle response code here if needed
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("FetchSteamIDTask", "Failed to fetch data: " + connection.getResponseCode());
                    return null; // Return null on failure
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                JSONObject responseObj = jsonResponse.getJSONObject("response");

                // Check if Steam ID was successfully retrieved
                if (responseObj.getInt("success") == 1) {
                    return responseObj.getString("steamid");
                } else {
                    return null;  // Vanity URL not found
                }
            } catch (Exception e) {
                Log.e("FetchSteamIDTask", "Error fetching Steam ID", e);
                return null;
            }
        }
        @Override
        protected void onPostExecute(String steamID) {
            if (steamID != null) {
                // Save the Steam ID in SharedPreferences
                sharedPreferences.edit().putString("steam_id", steamID).apply();
                steamIdTextView.setText(steamID); // Update the UI immediately
                Toast.makeText(SettingsActivity.this, "Steam ID saved successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SettingsActivity.this, "Steam name not found", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Register the listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener to prevent memory leaks
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }
}