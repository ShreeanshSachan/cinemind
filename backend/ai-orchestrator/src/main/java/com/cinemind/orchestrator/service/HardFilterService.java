package com.cinemind.orchestrator.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.rag.RagResult;

/**
 * Step 6: deterministic Java filtering. Simple constraints are never delegated
 * to an LLM, which keeps behaviour predictable and saves credits.
 */
@Component
public class HardFilterService {

    public List<RagResult> apply(List<RagResult> retrieved, MoviePreferences preferences) {
        if (retrieved == null) {
            return List.of();
        }
        return retrieved.stream()
                .filter(candidate -> candidate.getMovieId() != null)
                .filter(candidate -> passesYear(candidate, preferences.getReleaseYearMin()))
                .filter(candidate -> passesRuntime(candidate, preferences.getMaxRuntime()))
                .filter(candidate -> !isExcluded(candidate, preferences.getExcludedGenres()))
                .toList();
    }

    private static boolean passesYear(RagResult candidate, Integer releaseYearMin) {
        return releaseYearMin == null
                || (candidate.getReleaseYear() != null
                && candidate.getReleaseYear() >= releaseYearMin);
    }

    private static boolean passesRuntime(RagResult candidate, Integer maxRuntime) {
        return maxRuntime == null
                || (candidate.getRuntimeMinutes() != null
                && candidate.getRuntimeMinutes() <= maxRuntime);
    }

    private static boolean isExcluded(RagResult candidate, List<String> excludedGenres) {
        if (excludedGenres == null || excludedGenres.isEmpty()) {
            return false;
        }
        List<String> genres = candidate.getGenres();
        if (genres == null) {
            return false;
        }
        return excludedGenres.stream()
                .filter(value -> value != null && !value.isBlank())
                .anyMatch(excluded -> genres.stream()
                        .filter(genre -> genre != null)
                        .anyMatch(genre -> genre.toLowerCase()
                                .contains(excluded.trim().toLowerCase())));
    }
}
