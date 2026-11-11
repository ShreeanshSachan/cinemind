package com.cinemind.movie.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/movies and PUT /api/movies/{id}. Never exposes the entity. */
public class MovieRequest {

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @Size(max = 4000, message = "overview must be at most 4000 characters")
    private String overview;

    private List<String> genres = new ArrayList<>();

    @Min(value = 1888, message = "releaseYear must be 1888 or later")
    @Max(value = 2100, message = "releaseYear must be 2100 or earlier")
    private Integer releaseYear;

    @Min(value = 1, message = "runtimeMinutes must be positive")
    @Max(value = 1000, message = "runtimeMinutes must be at most 1000")
    private Integer runtimeMinutes;

    private List<String> keywords = new ArrayList<>();

    @DecimalMin(value = "0.0", message = "rating must be at least 0")
    @DecimalMax(value = "10.0", message = "rating must be at most 10")
    private BigDecimal rating;

    @Size(max = 512, message = "posterUrl must be at most 512 characters")
    private String posterUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public Integer getRuntimeMinutes() {
        return runtimeMinutes;
    }

    public void setRuntimeMinutes(Integer runtimeMinutes) {
        this.runtimeMinutes = runtimeMinutes;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }
}
