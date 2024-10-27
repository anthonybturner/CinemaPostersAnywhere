package com.anthonybturner.cinemapostersanywhere.interfaces;

import com.anthonybturner.cinemapostersanywhere.Models.ApexStats;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApexLegendsApi {
    @GET("/apex/v1/profile/{platform}/{player}")
    Call<ApexStats> getPlayerStats(
            @Path("platform") String platform,
            @Path("player") String player,
            @Header("TRN-Api-Key") String apiKey  // Add API key as query parameter
    );
}