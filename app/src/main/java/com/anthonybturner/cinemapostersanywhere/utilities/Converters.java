package com.anthonybturner.cinemapostersanywhere.utilities;

import androidx.room.TypeConverter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Converters {

    // Convert List<Integer> to a comma-separated String
    @TypeConverter
    public static String fromGenreIdsList(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return genreIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));  // Convert list to a comma-separated string
        } else {
            // For older versions, manually convert list to a comma-separated string
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < genreIds.size(); i++) {
                result.append(genreIds.get(i));
                if (i < genreIds.size() - 1) {
                    result.append(",");
                }
            }
            return result.toString();
        }
    }

    public static String convertUnixToReadable(long unixSeconds) {
        Date date = new Date(unixSeconds * 1000L);  // Convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }

    // Convert a comma-separated String back to List<Integer>
    @TypeConverter
    public static List<Integer> toGenreIdsList(String genreIdsString) {
        if (genreIdsString == null || genreIdsString.isEmpty()) {
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Arrays.stream(genreIdsString.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());  // Convert the string back to a list of integers
        } else {
            // For older versions, manually split and convert the string to a list of integers
            String[] ids = genreIdsString.split(",");
            List<Integer> genreIds = new ArrayList<>();
            for (String id : ids) {
                genreIds.add(Integer.parseInt(id.trim()));
            }
            return genreIds;
        }
    }
}
