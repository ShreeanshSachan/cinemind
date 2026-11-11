package com.cinemind.movie.dto;

import java.math.BigDecimal;

/** Query parameters accepted by GET /api/movies/filter. All are optional. */
public class MovieFilter {

    private String genre;
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
