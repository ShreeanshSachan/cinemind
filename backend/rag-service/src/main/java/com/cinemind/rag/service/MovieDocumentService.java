package com.cinemind.rag.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cinemind.rag.dto.MovieMetadata;

/**
 * Turns movie metadata into one concise text document for embedding.
 * Only stable, factual metadata is used - no LLM generates this text.
 */
@Service
public class MovieDocumentService {

    public String buildDocument(MovieMetadata movie) {
        if (movie == null) {
            throw new IllegalArgumentException("Movie metadata must not be null");
        }

        return "Title: " + text(movie.getTitle()) + ". "
                + "Overview: " + text(movie.getOverview()) + ". "
                + "Genres: " + join(movie.getGenres()) + ". "
                + "Release year: " + number(movie.getReleaseYear()) + ". "
                + "Runtime: " + number(movie.getRuntimeMinutes()) + " minutes. "
                + "Keywords: " + join(movie.getKeywords()) + ".";
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static String number(Integer value) {
        return value == null ? "unknown" : value.toString();
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "none";
        }
        return String.join(", ", values);
    }
}
