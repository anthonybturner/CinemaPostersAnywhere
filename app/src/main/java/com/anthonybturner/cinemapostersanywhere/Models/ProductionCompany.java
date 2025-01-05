package com.anthonybturner.cinemapostersanywhere.Models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "production_companies")
public class ProductionCompany {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String logo_path;
    private String name;
    private String origin_country;

    // Default constructor required by Room
    public ProductionCompany() {
    }

    public String getLogo_path() {
        return logo_path;
    }

    public void setLogo_path(String logo_path) {
        this.logo_path = logo_path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigin_country() {
        return origin_country;
    }

    public void setOrigin_country(String origin_country) {
        this.origin_country = origin_country;
    }
}
