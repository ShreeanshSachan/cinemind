package com.cinemind.orchestrator.dto.rag;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RagSearchResponse {

    private String query;
    private int topK;
    private int candidateCount;
    private int retrievedCount;
    private List<RagResult> results = new ArrayList<>();

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public void setCandidateCount(int candidateCount) {
        this.candidateCount = candidateCount;
    }

    public int getRetrievedCount() {
        return retrievedCount;
    }

    public void setRetrievedCount(int retrievedCount) {
        this.retrievedCount = retrievedCount;
    }

    public List<RagResult> getResults() {
        return results;
    }

    public void setResults(List<RagResult> results) {
        this.results = results == null ? new ArrayList<>() : results;
    }
}
