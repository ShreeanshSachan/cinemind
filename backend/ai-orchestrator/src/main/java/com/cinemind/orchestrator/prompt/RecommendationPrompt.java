package com.cinemind.orchestrator.prompt;

import java.util.List;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.rag.RagResult;

/**
 * Compact, single-shot prompt for Agent 2. Only id/title/year/runtime/genres/
 * rating/similarity are sent - never overviews, keywords, documents or posters.
 */
public final class RecommendationPrompt {

    private RecommendationPrompt() {
    }

    public static String build(MoviePreferences preferences, List<RagResult> candidates,
                               int maxResults, String outputFormat) {
        return """
                You are a movie recommendation specialist.
                Rank and explain ONLY the candidate movies listed below.

                Rules:
                - Use ONLY movieId values from the candidate list.
                - Never invent movie IDs, titles, years or any metadata.
                - Copy title and releaseYear exactly from the candidate list.
                - Return at most %d recommendations, best match first.
                - matchScore: integer from 0 to 100.
                - reason: ONE short sentence of at most 25 words.
                - Output ONLY a RFC8259 compliant JSON object. No prose, no markdown fences.
                - If no candidates are suitable, return an empty recommendations array.

                Preferences: %s

                Candidates (id | title | year | runtime | genres | rating | similarity):
                %s

                %s
                """.formatted(maxResults, describe(preferences), candidateLines(candidates), outputFormat);
    }

    private static String describe(MoviePreferences preferences) {
        return "genres=" + preferences.getGenres()
                + ", mood=" + preferences.getMood()
                + ", similarTo=" + preferences.getSimilarTo()
                + ", releaseYearMin=" + preferences.getReleaseYearMin()
                + ", maxRuntime=" + preferences.getMaxRuntime()
                + ", excludedGenres=" + preferences.getExcludedGenres();
    }

    private static String candidateLines(List<RagResult> candidates) {
        if (candidates.isEmpty()) {
            return "(no candidates)";
        }
        StringBuilder lines = new StringBuilder();
        for (RagResult candidate : candidates) {
            lines.append(candidate.getMovieId()).append(" | ")
                    .append(candidate.getTitle()).append(" | ")
                    .append(candidate.getReleaseYear()).append(" | ")
                    .append(candidate.getRuntimeMinutes()).append("min | ")
                    .append(String.join(",", candidate.getGenres())).append(" | ")
                    .append(candidate.getRating()).append(" | ")
                    .append(candidate.getSimilarityScore()).append('\n');
        }
        return lines.toString();
    }
}
