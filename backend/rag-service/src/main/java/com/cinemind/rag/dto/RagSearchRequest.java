package com.cinemind.rag.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class RagSearchRequest {

    @NotBlank(message = "query is required")
    private String query;

    /** Defaults to 10; clamped to the configured maximum in the service. */
    @Min(value = 1, message = "topK must be at least 1")
    @Max(value = 50, message = "topK must be at most 50")
    private Integer topK = 10;

    @DecimalMin(value = "-1.0", message = "minScore must be >= -1")
    @DecimalMax(value = "1.0", message = "minScore must be <= 1")
    private Double minScore = 0.0;

    @Valid
    private MovieMetadataFilter filters = new MovieMetadataFilter();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public MovieMetadataFilter getFilters() {
        return filters;
    }

    public void setFilters(MovieMetadataFilter filters) {
        this.filters = filters == null ? new MovieMetadataFilter() : filters;
    }
}
