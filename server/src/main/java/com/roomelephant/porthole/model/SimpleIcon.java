package com.roomelephant.porthole.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleIcon {
    private String title;
    private String slug;
    private String hex;

    // Constructors
    public SimpleIcon() {
    }

    public SimpleIcon(String title, String slug, String hex) {
        this.title = title;
        this.slug = slug;
        this.hex = hex;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getHex() {
        return hex;
    }

    public void setHex(String hex) {
        this.hex = hex;
    }
}
