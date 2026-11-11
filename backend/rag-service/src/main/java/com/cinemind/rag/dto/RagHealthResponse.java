package com.cinemind.rag.dto;

public record RagHealthResponse(
        String status,
        boolean ollamaAvailable,
        boolean modelAvailable,
        String embeddingModel,
        int embeddingDimension,
        long indexedMovies,
        String message) {
}
