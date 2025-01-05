package com.anthonybturner.cinemapostersanywhere.Models;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.anthonybturner.cinemapostersanywhere.utilities.Converters;
import com.anthonybturner.cinemapostersanywhere.utilities.SpokenLanguageTypeConverter;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;

@Entity(tableName = "movies")
public class Movie {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @SerializedName("movie_title")
    private String title;

    private String overview;
    @SerializedName("tagline")
    private String tagline;

    @SerializedName("category")
    private String category;

    @SerializedName("poster_url")
    private String posterPath;

    private String posterImage;  // Raw binary data for the poster

    @SerializedName("vote_average")
    private float voteAverage;  // Correct name for JSON mapping and Java convention

    public String[] getGenres() {
        return genres;
    }
    public void setGenres(String[] genres) {
        this.genres = genres;
    }
    @SerializedName("genres")
    @TypeConverters(Converters.class)
    private String[] genres;

    public List<ProductionCompany> getProductionCompanies() {
        return productionCompanies;
    }

    public void setProductionCompanies(List<ProductionCompany> productionCompanies) {
        this.productionCompanies = productionCompanies;
    }

    @SerializedName("production_companies")
    @TypeConverters(Converters.class)  // Use the converter for List<Integer>
    private List<ProductionCompany> productionCompanies;  // Genre IDs returned by the TMDB API

    @SerializedName("spoken_languages")
    @TypeConverters(SpokenLanguageTypeConverter.class)
    private List<SpokenLanguage> spokenLanguages;

    @SerializedName("adult")
    private Boolean adult;

    @SerializedName("original_language")
    private String originalLanguage;

    @SerializedName("popularity")
    private double popularity;

    @SerializedName("vote_count")
    private int voteCount;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("runtime")
    private Integer runtime;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @SerializedName("status")
    private String status;

    // Default constructor required by Room
    public Movie() {
    }

    // Constructor with title and overview
    public Movie(String title, String overview) {
        this.title = title;
        this.overview = overview;
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
        return posterPath;
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

    // Getter and setter for voteAverage
    public float getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(float voteAverage) {
        this.voteAverage = voteAverage;
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

            LocalDate localDate = LocalDate.parse(releaseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return String.valueOf(localDate.getYear());
        } catch (DateTimeParseException e) {
            Log.d("Movie", "Error parsing release date: " + releaseDate, e);
            return "Unknown Year";  // Return a default value in case of error
        }
    }

    // Getter and setter for adult
    public Boolean getAdult() {
        return adult;
    }

    public void setAdult(Boolean adult) {
        this.adult = adult;
    }

    // Getter and setter for originalLanguage
    public String getOriginalLanguage() {
        return originalLanguage;
    }

    public void setOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    // Add a getter for genreNames
    public String[] getGenreNames() {
        return genres;
    }

    // Add a setter for genreNames
    public void setGenreNames(String[] genreNames) {
        this.genres = genreNames;
    }
    // Getter and setter for popularity
    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    // Getter and setter for voteCount
    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public List<SpokenLanguage> getSpokenLanguages() {
       return spokenLanguages;
   }

   public void setSpokenLanguages(List<SpokenLanguage> spokenLanguages) {
        // Convert from List<String> to JSON string
        this.spokenLanguages = spokenLanguages;
   }

    public Integer getRuntime() {
        return runtime;
    }

    public void setRuntime(Integer runtime) {
        this.runtime = runtime;
    }
}