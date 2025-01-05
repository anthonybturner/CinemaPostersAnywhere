package com.anthonybturner.cinemapostersanywhere.utilities;

import androidx.room.TypeConverter;

import com.anthonybturner.cinemapostersanywhere.Models.ProductionCompany;
import com.anthonybturner.cinemapostersanywhere.Models.SpokenLanguage;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ProductionCompanyTypeConverter {
    @TypeConverter
    public static String fromProductionCompanyList(List<ProductionCompany> productionCompany) {
        if (productionCompany == null) {
            return null;
        }
        Gson gson = new Gson();
        return gson.toJson(productionCompany);  // Convert the list to JSON
    }

    @TypeConverter
    public static List<ProductionCompany> toProductionCompanyList(String productionCompanyJson) {
        if (productionCompanyJson == null) {
            return null;
        }
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ProductionCompany>>(){}.getType();
        return gson.fromJson(productionCompanyJson, listType);  // Convert the JSON back to a list
    }
}
