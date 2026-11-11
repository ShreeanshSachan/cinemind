package com.cinemind.rag.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cinemind.rag.dto.MovieMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MovieDocumentServiceTest {

    private final MovieDocumentService documentService = new MovieDocumentService();

    @Test
    void includesAllRequiredMetadata() {
        String document = documentService.buildDocument(metadata());

        assertThat(document).contains("Title: Shutter Island");
        assertThat(document).contains("Overview: A U.S. Marshal investigates an island asylum.");
        assertThat(document).contains("Genres: Mystery, Thriller");
        assertThat(document).contains("Release year: 2010");
        assertThat(document).contains("Runtime: 138 minutes");
        assertThat(document).contains("Keywords: asylum, paranoia");
    }

    @Test
    void documentIsConcise() {
        assertThat(documentService.buildDocument(metadata()).length()).isLessThan(400);
    }

    @Test
    void handlesMissingOptionalValues() {
        MovieMetadata movie = new MovieMetadata();
        movie.setTitle("Minimal Movie");

        String document = documentService.buildDocument(movie);

        assertThat(document).contains("unknown");
        assertThat(document).contains("Genres: none");
        assertThat(document).contains("Keywords: none");
    }

    @Test
    void rejectsNullMovie() {
        assertThatThrownBy(() -> documentService.buildDocument(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MovieMetadata metadata() {
        MovieMetadata movie = new MovieMetadata();
        movie.setId(17L);
        movie.setTitle("Shutter Island");
        movie.setOverview("A U.S. Marshal investigates an island asylum.");
        movie.setGenres(List.of("Mystery", "Thriller"));
        movie.setReleaseYear(2010);
        movie.setRuntimeMinutes(138);
        movie.setKeywords(List.of("asylum", "paranoia"));
        movie.setRating(new BigDecimal("8.2"));
        return movie;
    }
}
