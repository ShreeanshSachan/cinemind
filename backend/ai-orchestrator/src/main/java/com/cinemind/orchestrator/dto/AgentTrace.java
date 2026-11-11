package com.cinemind.orchestrator.dto;

/** Execution statuses plus retrieval counts, taken from the real workflow. */
public record AgentTrace(
        String preferenceAgent,
        String ragRetrieval,
        String recommendationAgent,
        int retrievedDocuments,
        int candidatesAfterFilters) {
}
