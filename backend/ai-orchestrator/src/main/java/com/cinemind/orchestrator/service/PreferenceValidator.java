package com.cinemind.orchestrator.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.exception.InvalidPreferencesException;

/**
 * Step 3: validate Agent 1's output in plain Java. No extra LLM call is used;
 * values are normalised and clamped so the rest of the workflow is deterministic.
 */
@Component
public class PreferenceValidator {

    private static final int MAX_LIST_SIZE = 8;
    private static final int MAX_TEXT_LENGTH = 120;

    public MoviePreferences validateAndNormalize(MoviePreferences raw) {
        if (raw == null) {
            throw new InvalidPreferencesException("Preference Analyst produced no preferences");
        }

        MoviePreferences preferences = new MoviePreferences();
        preferences.setGenres(cleanList(raw.getGenres()));
        preferences.setSimilarTo(cleanList(raw.getSimilarTo()));
        preferences.setExcludedGenres(cleanList(raw.getExcludedGenres()));
        preferences.setMood(cleanText(raw.getMood()));
        preferences.setReleaseYearMin(clamp(raw.getReleaseYearMin(), 1888, 2100));
        preferences.setMaxRuntime(clamp(raw.getMaxRuntime(), 1, 1000));
        return preferences;
    }

    private static List<String> cleanList(List<String> values) {
        List<String> cleaned = new ArrayList<>();
        if (values == null) {
            return cleaned;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim().substring(0, Math.min(value.trim().length(), MAX_TEXT_LENGTH));
            if (!cleaned.contains(trimmed)) {
                cleaned.add(trimmed);
            }
            if (cleaned.size() == MAX_LIST_SIZE) {
                break;
            }
        }
        return cleaned;
    }

    private static String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), MAX_TEXT_LENGTH));
    }

    private static Integer clamp(Integer value, int min, int max) {
        if (value == null) {
            return null;
        }
        return Math.min(Math.max(value, min), max);
    }
}
