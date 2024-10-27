package com.anthonybturner.cinemapostersanywhere;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.anthonybturner.cinemapostersanywhere.Models.ApexStats;
import com.anthonybturner.cinemapostersanywhere.Models.YouTubeResponse;
import com.anthonybturner.cinemapostersanywhere.interfaces.ApexLegendsApi;
import com.anthonybturner.cinemapostersanywhere.interfaces.YouTubeApiService;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApexLegendsActivity  extends AppCompatActivity {

    private TextView statsTextView;

    private static final String BASE_URL = "https://public-api.tracker.gg/v2/";
    private static final String APEX_TRACKER_API_KEY = "301499fb-67dd-41bb-a430-9fbeaa19fb57"; // Your YouTube API key

    private static Retrofit retrofit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apex_legends);
        statsTextView = findViewById(R.id.overview);
        // Initialize API client
       fetchApexStats("origin", "silentcod3r");

    }

    private void fetchApexStats(String platform, String playerName) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApexLegendsApi apiService = retrofit.create(ApexLegendsApi.class);
        Call<ApexStats> call = apiService.getPlayerStats(platform, playerName, APEX_TRACKER_API_KEY);
        call.enqueue(new Callback<ApexStats>() {
            @Override
            public void onResponse(Call<ApexStats> call, Response<ApexStats> response) {
                if (response.isSuccessful()) {
                    ApexStats stats = response.body();
                    if (stats != null) {
                        statsTextView.setText(String.format(Locale.ENGLISH, "Legend: %s\nKills: %d\nDamage: %d", stats.getLegendName(), stats.getKills(), stats.getDamage()));
                    }
                }
            }
            @Override
            public void onFailure(Call<ApexStats> call, Throwable t) {
                statsTextView.setText("Failed to load stats.");
            }
        });
    }
}