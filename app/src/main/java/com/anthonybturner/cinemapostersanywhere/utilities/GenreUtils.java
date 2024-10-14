package com.anthonybturner.cinemapostersanywhere.utilities;

import android.os.Build;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GenreUtils {

    // Initialize the genre map using HashMap
    private static final Map<Integer, String> genreMap = new HashMap<>();

    static {
        genreMap.put(28, "Action");
        genreMap.put(12, "Adventure");
        genreMap.put(16, "Animation");
        genreMap.put(35, "Comedy");
        genreMap.put(80, "Crime");
        genreMap.put(99, "Documentary");
        genreMap.put(18, "Drama");
        genreMap.put(10751, "Family");
        genreMap.put(14, "Fantasy");
        genreMap.put(36, "History");
        genreMap.put(27, "Horror");
        genreMap.put(10402, "Music");
        genreMap.put(9648, "Mystery");
        genreMap.put(10749, "Romance");
        genreMap.put(878, "Science Fiction");
        genreMap.put(53, "Thriller");
        genreMap.put(10752, "War");
        genreMap.put(37, "Western");
    }

    // Convert genre IDs to a comma-separated genre string
    public static String convertGenreIdsToNames(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return "";  // Return empty string if genreIds is null or empty
        }

        // For Android versions >= Nougat (API 24)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return genreIds.stream()
                    .map(genreMap::get)  // Map each genre ID to its name
                    .filter(Objects::nonNull)  // Filter out nulls in case of unmapped IDs
                    .collect(Collectors.joining(", "));  // Join names with a comma and space
        } else {
            // For Android versions < Nougat
            List<String> genreNames = new ArrayList<>();
            for (Integer genreId : genreIds) {
                String genreName = genreMap.get(genreId);
                if (genreName != null) {
                    genreNames.add(genreName);
                }
            }
            return String.join(", ", genreNames);  // Manually join names with comma and space
        }
    }
}
