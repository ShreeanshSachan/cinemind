package com.cinemind.orchestrator.service;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.exception.InvalidPreferencesException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreferenceValidatorTest {

    private final PreferenceValidator validator = new PreferenceValidator();

    @Test
    void normalisesNullsToSafeDefaults() {
        MoviePreferences raw = new MoviePreferences();
        raw.setGenres(null);
        raw.setSimilarTo(null);
        raw.setExcludedGenres(null);
        raw.setMood(null);

        MoviePreferences result = validator.validateAndNormalize(raw);

        assertThat(result.getGenres()).isEmpty();
        assertThat(result.getSimilarTo()).isEmpty();
        assertThat(result.getExcludedGenres()).isEmpty();
        assertThat(result.getMood()).isEmpty();
        assertThat(result.getReleaseYearMin()).isNull();
        assertThat(result.getMaxRuntime()).isNull();
    }

    @Test
    void clampsOutOfRangeNumbers() {
        MoviePreferences raw = new MoviePreferences();
        raw.setReleaseYearMin(1200);
        raw.setMaxRuntime(5000);

        MoviePreferences result = validator.validateAndNormalize(raw);

        assertThat(result.getReleaseYearMin()).isEqualTo(1888);
        assertThat(result.getMaxRuntime()).isEqualTo(1000);
    }

    @Test
    void trimsDeduplicatesAndCapsLists() {
        MoviePreferences raw = new MoviePreferences();
        raw.setGenres(Arrays.asList(" Thriller ", "", "Thriller", "Drama", null,
                "A", "B", "C", "D", "E", "F", "G", "H", "I"));

        MoviePreferences result = validator.validateAndNormalize(raw);

        assertThat(result.getGenres()).startsWith("Thriller", "Drama");
        assertThat(result.getGenres()).hasSize(8);
        assertThat(result.getGenres()).doesNotContain("");
    }

    @Test
    void rejectsNullOutput() {
        assertThatThrownBy(() -> validator.validateAndNormalize(null))
                .isInstanceOf(InvalidPreferencesException.class);
    }
}
