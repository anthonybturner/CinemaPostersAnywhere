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
import com.anthonybturner.cinemapostersanywhere.Constants.Steam;
import com.anthonybturner.cinemapostersanywhere.Constants.SharedPrefsConstants;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsActivity extends AppCompatActivity {

    private EditText kodiIpText, kodiPortText;
    private EditText steamNameEditText;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private TextView steamIdTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        steamNameEditText = findViewById(R.id.steamNameEditText);
        steamIdTextView = findViewById(R.id.steamId); // Assuming you have a TextView for Steam ID


        kodiIpText = findViewById(R.id.kodiIpAddress);
        kodiPortText = findViewById(R.id.kodiPortAddress);

        Button saveButton = findViewById(R.id.saveButton);
        sharedPreferences = getSharedPreferences(SharedPrefsConstants.PREFS_KEY_APP_PREFERENCES, MODE_PRIVATE);
        // Initialize the listener
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(SharedPrefsConstants.PREFS_KEY_STEAM_ID)) {
                    String newSteamID = sharedPreferences.getString(key, null);
                    Log.d("SettingsActivity", "New Steam ID: " + newSteamID);
                    // Update UI or perform any action needed
                    SteamGameService.setSteamId(newSteamID);
                }
            }
        };
        loadSteamPrefs();
        loadKodiPrefs();

        saveButton.setOnClickListener(v -> {
               saveSteamPrefs();
               saveKodiPrefs();
        });
    }

    private void loadKodiPrefs() {
        // Retrieve the saved Steam name
        String prefValue = sharedPreferences.getString(SharedPrefsConstants.PREF_KEY_KODI_IP_ADDRESS, null);
        if (prefValue != null) {
            kodiIpText.setText(prefValue); // Set the EditText to the saved Steam name
        }
        prefValue = sharedPreferences.getString(SharedPrefsConstants.PREF_KEY_KODI_PORT, null);
        if (prefValue != null) {
            kodiPortText.setText(prefValue); // Set the EditText to the saved Steam name
        }
    }
    private void saveKodiPrefs() {
        String ip = kodiIpText.getText().toString();
        String port = kodiPortText.getText().toString();
        sharedPreferences.edit().putString(SharedPrefsConstants.PREF_KEY_KODI_IP_ADDRESS, ip).apply();
        sharedPreferences.edit().putString(SharedPrefsConstants.PREF_KEY_KODI_PORT, port).apply();
        Toast.makeText(this, "Kodi settings saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void loadSteamPrefs(){
        // Retrieve the saved Steam name
        String prefValue = sharedPreferences.getString(SharedPrefsConstants.PREF_KEY_STEAM_NAME_KEY, null);
        if (prefValue != null) {
            steamNameEditText.setText(prefValue); // Set the EditText to the saved Steam name
        }
        // Retrieve the saved Steam ID
        prefValue = sharedPreferences.getString(SharedPrefsConstants.PREFS_KEY_STEAM_ID, null);
        if (prefValue != null) {
            steamIdTextView.setText(prefValue); // Update the UI to show the Steam ID
        }
    }

    private void saveSteamPrefs() {
        String prefValue = steamNameEditText.getText().toString();
        if(prefValue.isEmpty()){
            Toast.makeText(SettingsActivity.this, "Please enter a Steam name", Toast.LENGTH_SHORT).show();
        }else{
            // Save the Steam name in SharedPreferences
            sharedPreferences.edit().putString(SharedPrefsConstants.PREF_KEY_STEAM_NAME_KEY, prefValue).apply();
            Toast.makeText(this, "Steam name saved successfully", Toast.LENGTH_SHORT).show();
            fetchSteamID(prefValue);//Goto the steam api to get the steam name's steam id.
        }
    }

    private void fetchSteamID(String prefValue) {
        new FetchSteamIDTask().execute(prefValue);
    }

    private class FetchSteamIDTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String steamName = params[0];
            String apiUrl = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=" + Steam.STEAM_API_KEY + "&vanityurl=" + steamName;

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
                sharedPreferences.edit().putString(SharedPrefsConstants.PREFS_KEY_STEAM_ID, steamID).apply();
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