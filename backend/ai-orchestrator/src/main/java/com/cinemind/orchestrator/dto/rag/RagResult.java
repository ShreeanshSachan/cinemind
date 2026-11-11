package com.cinemind.orchestrator.dto.rag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One retrieved movie document from the RAG Service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RagResult {

    private Long movieId;
    private String title;
    private List<String> genres = new ArrayList<>();
    private Integer releaseYear;
    private Integer runtimeMinutes;
    private BigDecimal rating;
    private double similarityScore;
    private String documentContent;

    public RagResult() {
    }

    public RagResult(Long movieId, String title, List<String> genres, Integer releaseYear,
                     Integer runtimeMinutes, BigDecimal rating, double similarityScore) {
        this.movieId = movieId;
        this.title = title;
        this.genres = genres == null ? new ArrayList<>() : genres;
        this.releaseYear = releaseYear;
        this.runtimeMinutes = runtimeMinutes;
        this.rating = rating;
        this.similarityScore = similarityScore;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres == null ? new ArrayList<>() : genres;
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

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getDocumentContent() {
        return documentContent;
    }

    public void setDocumentContent(String documentContent) {
        this.documentContent = documentContent;
    }
}
