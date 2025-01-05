package com.anthonybturner.cinemapostersanywhere.utilities;

import androidx.room.TypeConverter;

import com.anthonybturner.cinemapostersanywhere.Models.SpokenLanguage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class SpokenLanguageTypeConverter {

    @TypeConverter
    public static String fromSpokenLanguagesList(List<SpokenLanguage> spokenLanguages) {
        if (spokenLanguages == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(spokenLanguages);  // Convert the list to JSON
    }

    @TypeConverter
    public static List<SpokenLanguage> toSpokenLanguagesList(String spokenLanguagesJson) {
        if (spokenLanguagesJson == null) {
            return null;
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<SpokenLanguage>>(){}.getType();
        return gson.fromJson(spokenLanguagesJson, listType);  // Convert the JSON back to a list
    }
}
