package com.cinemind.rag.dto;

public record BulkIndexResult(
        int totalMovies,
        int created,
        int updated,
        int skipped,
        String modelName) {
}
