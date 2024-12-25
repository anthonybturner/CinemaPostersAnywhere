package com.anthonybturner.cinemapostersanywhere.interfaces;

import com.anthonybturner.cinemapostersanywhere.Models.YouTubeResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

// Define YouTube API service
public interface YouTubeApiService {
    @GET("search")
    Call<YouTubeResponse> getRelatedVideos(
            @Query("q") String query,
            @Query("key") String apiKey,
            @Query("part") String part,       // Removed default value
            @Query("type") String type,       // Removed default value
            @Query("maxResults") int maxResults // Removed default value
    );
}
