package com.anthonybturner.cinemapostersanywhere.Models;

import com.google.gson.annotations.SerializedName;

public class YouTubeVideo {
    @SerializedName("id")
    private YouTubeVideoId id;

    @SerializedName("snippet")
    private Snippet snippet;

    public YouTubeVideoId getId() {
        return id;
    }

    public Snippet getSnippet() {
        return snippet; // Add this getter for Snippet
    }

    public static class YouTubeVideoId {
        @SerializedName("videoId")
        private String videoId;

        public String getVideoId() {
            return videoId;
        }
    }

    public static class Snippet {
        @SerializedName("title")
        private String title;

        @SerializedName("description")
        private String description;

        @SerializedName("thumbnails")
        private Thumbnails thumbnails;

        @SerializedName("channelTitle")
        private String channelTitle;

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Thumbnails getThumbnails() {
            return thumbnails;
        }

        public String getChannelTitle() {
            return channelTitle;
        }
    }

    public static class Thumbnails {
        @SerializedName("default")
        private Thumbnail defaultThumbnail;

        @SerializedName("medium")
        private Thumbnail medium;

        @SerializedName("high")
        private Thumbnail high;

        public Thumbnail getDefault() {
            return defaultThumbnail;
        }

        public Thumbnail getMedium() {
            return medium;
        }

        public Thumbnail getHigh() {
            return high;
        }
    }

    public static class Thumbnail {
        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }
    }
}
