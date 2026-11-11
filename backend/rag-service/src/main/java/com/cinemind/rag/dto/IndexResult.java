package com.cinemind.rag.dto;

public record IndexResult(
        Long movieId,
        String title,
        IndexStatus status,
        int dimensions,
        String message) {
}
