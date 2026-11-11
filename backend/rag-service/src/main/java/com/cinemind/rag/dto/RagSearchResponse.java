package com.cinemind.rag.dto;

import java.util.List;

public record RagSearchResponse(
        String query,
        int topK,
        int candidateCount,
        int retrievedCount,
        MovieMetadataFilter appliedFilters,
        List<RetrievedMovie> results) {
}
