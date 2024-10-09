package com.anthonybturner.cinemapostersanywhere.Models;

import com.google.gson.annotations.SerializedName;

public class NowPlayingMovie {
    private final String title;
    private final int year;

    @SerializedName("poster_path")
    private String posterUrl;

    public NowPlayingMovie(String title, int year, String posterUrl) {
        this.title = title;
        this.year = year;
        this.posterUrl = posterUrl;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public String getPosterUrl() { return posterUrl; }
}
