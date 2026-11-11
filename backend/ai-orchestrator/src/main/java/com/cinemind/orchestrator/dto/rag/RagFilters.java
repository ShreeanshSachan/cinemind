package com.cinemind.orchestrator.dto.rag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Metadata filters sent to POST /api/rag/search. */
public class RagFilters {

    private String genre;
    private List<String> excludedGenres = new ArrayList<>();
    private Integer yearFrom;
    private Integer yearTo;
    private Integer minRuntime;
    private Integer maxRuntime;
    private BigDecimal minRating;

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public List<String> getExcludedGenres() {
        return excludedGenres;
    }

    public void setExcludedGenres(List<String> excludedGenres) {
        this.excludedGenres = excludedGenres == null ? new ArrayList<>() : excludedGenres;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public void setYearFrom(Integer yearFrom) {
        this.yearFrom = yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public void setYearTo(Integer yearTo) {
        this.yearTo = yearTo;
    }

    public Integer getMinRuntime() {
        return minRuntime;
    }

    public void setMinRuntime(Integer minRuntime) {
        this.minRuntime = minRuntime;
    }

    public Integer getMaxRuntime() {
        return maxRuntime;
    }

    public void setMaxRuntime(Integer maxRuntime) {
        this.maxRuntime = maxRuntime;
    }

    public BigDecimal getMinRating() {
        return minRating;
    }

    public void setMinRating(BigDecimal minRating) {
        this.minRating = minRating;
    }
}
