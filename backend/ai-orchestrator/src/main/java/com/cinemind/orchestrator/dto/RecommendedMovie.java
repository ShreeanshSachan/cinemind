package com.cinemind.orchestrator.dto;

/** One final recommendation returned to the caller. */
public record RecommendedMovie(
        Long movieId,
        String title,
        Integer releaseYear,
        int matchScore,
        String reason) {
}
