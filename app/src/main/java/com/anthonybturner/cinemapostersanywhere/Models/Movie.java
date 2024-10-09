package com.anthonybturner.cinemapostersanywhere.Models;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Entity(tableName = "movies")  // Define this class as a table in the Room database
public class Movie {

    @PrimaryKey(autoGenerate = true)  // Room Primary Key (auto-generated if needed)
    private int id;  // Room-generated ID

    @SerializedName("movie_title")  // Mapping for JSON
    private String title;

    private String overview;

    @SerializedName("category")  // Mapping for JSON
    private String category;

    @SerializedName("poster_url")  // Mapping for JSON
    private String posterPath;

    private String posterImage; // Raw binary data for the poster

    @SerializedName("release_year")  // Mapping for JSON
    private String releaseDate;

    // Default constructor required by Room
    public Movie() {
    }

    // Constructor with title and overview
    public Movie(String title, String overview) {
        this.title = title;
        this.overview = overview;
    }

    // Constructor with title, overview, and category
    public Movie(String title, String overview, String category) {
        this.title = title;
        this.overview = overview;
        this.category = category;
    }

    // Getter and setter for Room-generated ID
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Getter and setter for title
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Getter and setter for overview
    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    // Getter and setter for category
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // Getter and setter for posterPath
    public String getPosterPath() {
        return  posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    // Getter and setter for posterImage (binary data)
    public String getPosterImage() {
        return posterImage;
    }

    public void setPosterImage(String posterImage) {
        this.posterImage = posterImage;
    }

    // Getter and setter for releaseDate
    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    // Extract and return the release year from the releaseDate string
    @RequiresApi(api = Build.VERSION_CODES.O)
    public String getReleaseDateYear() {
        if (releaseDate == null || releaseDate.isEmpty()) {
            return "Unknown Year";  // Handle unknown year
        }

        try {
            if (releaseDate.length() == 4) {
                return releaseDate;  // If the releaseDate is just a year (e.g., "2024")
            }

            // Parse dates in the format "yyyy-MM-dd"
            LocalDate localDate = LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return String.valueOf(localDate.getYear());
        } catch (DateTimeParseException e) {
            Log.d("Movie", "Error parsing release date: " + releaseDate, e);
            return "Unknown Year";  // Return a default value in case of error
        }
    }
}