package com.cinemind.orchestrator.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.rag.RagResult;

import static org.assertj.core.api.Assertions.assertThat;

class HardFilterServiceTest {

    private final HardFilterService filterService = new HardFilterService();

    @Test
    void appliesYearRuntimeAndExcludedGenres() {
        MoviePreferences preferences = new MoviePreferences();
        preferences.setReleaseYearMin(2010);
        preferences.setMaxRuntime(140);
        preferences.setExcludedGenres(List.of("Horror"));

        List<RagResult> retrieved = List.of(
                result(1L, 2005, 120, List.of("Drama")),        // too old
                result(2L, 2015, 160, List.of("Thriller")),     // too long
                result(3L, 2012, 110, List.of("Horror")),       // excluded genre
                result(4L, 2011, 120, List.of("Thriller")));    // passes

        List<RagResult> filtered = filterService.apply(retrieved, preferences);

        assertThat(filtered).extracting(RagResult::getMovieId).containsExactly(4L);
    }

    @Test
    void noConstraintsKeepsEverythingExceptNullIds() {
        List<RagResult> retrieved = List.of(
                result(1L, 1999, 200, List.of("Drama")),
                result(null, 2010, 100, List.of("Thriller")));

        assertThat(filterService.apply(retrieved, new MoviePreferences()))
                .extracting(RagResult::getMovieId)
                .containsExactly(1L);
    }

    @Test
    void nullRetrievedListIsSafe() {
        assertThat(filterService.apply(null, new MoviePreferences())).isEmpty();
    }

    @Test
    void excludedGenreMatchIsCaseInsensitive() {
        MoviePreferences preferences = new MoviePreferences();
        preferences.setExcludedGenres(List.of("HORROR"));
        List<RagResult> retrieved = List.of(
                result(1L, 2010, 120, List.of("Psychological Horror")));

        assertThat(filterService.apply(retrieved, preferences)).isEmpty();
    }

    private static RagResult result(Long id, int year, int runtime, List<String> genres) {
        return new RagResult(id, "Movie " + id, genres, year, runtime,
                new BigDecimal("8.0"), 0.5);
    }
}
