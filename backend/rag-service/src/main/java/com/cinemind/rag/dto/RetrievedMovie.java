package com.cinemind.rag.dto;

import java.math.BigDecimal;
import java.util.List;

/** One retrieved movie document plus its score and source metadata. */
public record RetrievedMovie(
        Long movieId,
        String title,
        String overview,
        List<String> genres,
        Integer releaseYear,
        Integer runtimeMinutes,
        BigDecimal rating,
        String posterUrl,
        String documentContent,
        double similarityScore,
        String modelName) {
}
