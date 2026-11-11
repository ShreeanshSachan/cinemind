package com.cinemind.orchestrator.dto;

import java.util.List;

public record RecommendationResult(
        String query,
        int maxResults,
        int retrievedDocuments,
        MoviePreferences preferences,
        List<RecommendedMovie> recommendations,
        AgentTrace agentTrace) {
}
