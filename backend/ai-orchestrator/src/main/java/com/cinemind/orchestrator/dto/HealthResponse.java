package com.cinemind.orchestrator.dto;

public record HealthResponse(
        String status,
        String llmModel,
        boolean geminiConfigured,
        int maxGeminiCallsPerRequest) {
}
