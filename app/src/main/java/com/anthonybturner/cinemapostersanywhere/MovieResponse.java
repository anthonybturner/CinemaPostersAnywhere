package com.anthonybturner.cinemapostersanywhere;

import com.anthonybturner.cinemapostersanywhere.Models.Movie;

import java.util.List;

public class MovieResponse {
    private final List<Movie> results;

    public MovieResponse(List<Movie> results) {
        this.results = results;
    }

    public List<Movie> getResults() {
        return results;
    }
}
