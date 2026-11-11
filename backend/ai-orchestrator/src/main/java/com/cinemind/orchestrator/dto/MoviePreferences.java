package com.cinemind.orchestrator.dto;

import java.util.ArrayList;
import java.util.List;

/** Structured output of the Preference Analyst (Agent 1). */
public class MoviePreferences {

    private List<String> genres = new ArrayList<>();
    private String mood;
    private List<String> similarTo = new ArrayList<>();
    private Integer releaseYearMin;
    private Integer maxRuntime;
    private List<String> excludedGenres = new ArrayList<>();

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres == null ? new ArrayList<>() : genres;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public List<String> getSimilarTo() {
        return similarTo;
    }

    public void setSimilarTo(List<String> similarTo) {
        this.similarTo = similarTo == null ? new ArrayList<>() : similarTo;
    }

    public Integer getReleaseYearMin() {
        return releaseYearMin;
    }

    public void setReleaseYearMin(Integer releaseYearMin) {
        this.releaseYearMin = releaseYearMin;
    }

    public Integer getMaxRuntime() {
        return maxRuntime;
    }

    public void setMaxRuntime(Integer maxRuntime) {
        this.maxRuntime = maxRuntime;
    }

    public List<String> getExcludedGenres() {
        return excludedGenres;
    }

    public void setExcludedGenres(List<String> excludedGenres) {
        this.excludedGenres = excludedGenres == null ? new ArrayList<>() : excludedGenres;
    }
}
