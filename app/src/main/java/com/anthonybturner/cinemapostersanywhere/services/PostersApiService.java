package com.anthonybturner.cinemapostersanywhere.services;

import com.anthonybturner.cinemapostersanywhere.Models.Movie;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface PostersApiService {

    @GET("/api/posters")
    Call<List<Movie>> getPosters();  // Fetch trending movies from Flask API
    @GET("/api/fetch-trending")
    Call<List<Movie>> getTrendingPosters();  // Fetch trending movies from Flask API
    @GET("/api/fetch-popular")
    Call<List<Movie>> getPopularPosters();  // Fetch trending movies from Flask API
    @GET("/api/fetch-top-rated")
    Call<List<Movie>> getTopRatedPosters();  // Fetch trending movies from Flask API
}
