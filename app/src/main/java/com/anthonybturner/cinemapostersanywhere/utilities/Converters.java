package com.anthonybturner.cinemapostersanywhere.utilities;

import android.util.Log;

import androidx.room.TypeConverter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
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
    public static String decodeUrl(String url) {
        try {
            // Decode the URL
            return URLDecoder.decode(url, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return url;  // Return the original if decoding fails
        }
    }

    public static String convertUnixToReadable(long unixSeconds) {
        Date date = new Date(unixSeconds * 1000L);  // Convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(date);
    }

    public static String convertToFriendlyDate(String readableDate) {
        try {

            if(readableDate == null || readableDate.isEmpty()) return "";
            // Parse the input date in "yyyy-MM-dd HH:mm:ss" format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));  // Assuming date is in UTC if not local time
            Date date = inputFormat.parse(readableDate);
            // Format the date into "MMMM dd, yyyy hh:mm a" (e.g., "October 28, 2024 02:00 AM")
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy hh:mm a", Locale.getDefault());
            outputFormat.setTimeZone(TimeZone.getDefault());  // Use device's timezone for friendly display
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e("Converters", "Error converting date: " + e.getMessage());
            return readableDate; // Return the original if parsing fails
        }
    }
    public static String CalculateTime(long millisUntilFinished) {
        long totalSeconds = millisUntilFinished / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.ENGLISH, "Time: %02dh:%02dm:%02ds", hours, minutes, seconds);
    }
    public static String convertMinutesToHours(long totalMinutes) {
        long hours = totalMinutes / 60; // Calculate hours
        long minutes = totalMinutes % 60; // Calculate remaining minutes

        // Format the result as "X hours Y minutes"
        return hours + " hours " + minutes + " minutes";
    }
    public static String convertSecondsToTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        return String.format(Locale.ENGLISH, "%d:%d:%d", hours, minutes, remainingSeconds);
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
