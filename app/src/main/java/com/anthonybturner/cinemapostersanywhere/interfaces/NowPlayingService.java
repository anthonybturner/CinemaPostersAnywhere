package com.anthonybturner.cinemapostersanywhere.interfaces;

import com.anthonybturner.cinemapostersanywhere.Models.NowPlayingMovie;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NowPlayingService {
    @GET("/plex-webhook")
    Call<NowPlayingMovie> getNowPlaying();
}
