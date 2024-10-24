package com.anthonybturner.cinemapostersanywhere.Models;

import com.anthonybturner.cinemapostersanywhere.SteamGameActivity;
import com.google.gson.annotations.SerializedName;

import java.util.List;

// Define response classes
public class YouTubeResponse {
    @SerializedName("items")
    private List<YouTubeVideo> items;

    public List<YouTubeVideo> getItems() {
        return items;
    }
}
