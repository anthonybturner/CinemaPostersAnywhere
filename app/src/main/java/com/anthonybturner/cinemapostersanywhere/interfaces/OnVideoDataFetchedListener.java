package com.anthonybturner.cinemapostersanywhere.interfaces;

import com.anthonybturner.cinemapostersanywhere.Models.Video;

import java.util.List;

public interface OnVideoDataFetchedListener {
    void onVideoDataFetched(List<Video> videoList);
}
