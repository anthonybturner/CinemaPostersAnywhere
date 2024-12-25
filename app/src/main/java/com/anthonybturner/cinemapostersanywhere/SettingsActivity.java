/**
 *
 * This file is responsible for the settings activity. It allows the user to input the IP address and port number of their Kodi device.
 */
package com.anthonybturner.cinemapostersanywhere;

import android.content.SharedPreferences;

import android.os.Bundle;

import android.widget.Button;
import android.widget.EditText;

import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.anthonybturner.cinemapostersanywhere.Constants.SharedPrefsConstants;


public class SettingsActivity extends AppCompatActivity {

    private EditText kodiIpText, kodiPortText;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        kodiIpText = findViewById(R.id.kodiIpAddress);
        kodiPortText = findViewById(R.id.kodiPortAddress);

        Button saveButton = findViewById(R.id.saveButton);
        sharedPreferences = getSharedPreferences(SharedPrefsConstants.PREFS_KEY_APP_PREFERENCES, MODE_PRIVATE);
        // Initialize the listener
        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

            }
        };
        loadKodiPrefs();
        saveButton.setOnClickListener(v -> {
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