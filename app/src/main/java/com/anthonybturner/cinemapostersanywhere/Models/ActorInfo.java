package com.anthonybturner.cinemapostersanywhere.Models;

public class ActorInfo {
    private String role;
    private String thumb;

    public ActorInfo(String role, String thumb) {
        this.role = role;
        this.thumb = thumb;
    }

    public String getRole() {
        return role;
    }

    public String getThumb() {
        return thumb;
    }
}
