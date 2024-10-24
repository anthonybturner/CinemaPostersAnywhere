package com.anthonybturner.cinemapostersanywhere.Models;

public class Video {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private String description;
    private String channelTitle;

    public Video(String videoId, String title, String thumbnailUrl, String description, String channelTitle) {
        this.videoId = videoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.description = description;
        this.channelTitle = channelTitle;
    }

    // Getters
    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getChannelTitle() {
        return channelTitle;
    }
}
