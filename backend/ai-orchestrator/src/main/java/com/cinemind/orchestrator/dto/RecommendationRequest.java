package com.cinemind.orchestrator.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RecommendationRequest {

    @NotBlank(message = "query is required")
    @Size(max = 500, message = "query must be at most 500 characters")
    private String query;

    /** Clamped to 1..5 by the orchestrator. */
    @Min(value = 1, message = "maxResults must be at least 1")
    @Max(value = 5, message = "maxResults must be at most 5")
    private Integer maxResults = 5;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }
}
