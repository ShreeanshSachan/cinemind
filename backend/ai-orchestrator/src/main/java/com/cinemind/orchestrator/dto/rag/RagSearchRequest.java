package com.cinemind.orchestrator.dto.rag;

public class RagSearchRequest {

    private String query;
    private Integer topK = 10;
    private Double minScore = 0.0;
    private RagFilters filters = new RagFilters();

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

    public RagFilters getFilters() {
        return filters;
    }

    public void setFilters(RagFilters filters) {
        this.filters = filters;
    }
}
