package com.cinemind.movie.dto;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.cinemind.movie.entity.Movie;

import static org.assertj.core.api.Assertions.assertThat;

class MovieMapperTest {

    @Test
    void splitReturnsEmptyListForBlankInput() {
        assertThat(MovieMapper.split(null)).isEmpty();
        assertThat(MovieMapper.split("   ")).isEmpty();
    }

    @Test
    void splitTrimsAndDropsEmptyParts() {
        assertThat(MovieMapper.split(" Drama , ,Thriller ,"))
                .containsExactly("Drama", "Thriller");
    }

    @Test
    void joinAndSplitRoundTrip() {
        List<String> values = List.of("Science Fiction", "Thriller", "Action");
        assertThat(MovieMapper.split(MovieMapper.join(values))).containsExactlyElementsOf(values);
    }

    @Test
    void toResponseExposesGenresAndKeywordsAsArrays() {
        Movie movie = new Movie();
        movie.setId(7L);
        movie.setTitle("Inception");
        movie.setGenres("Science Fiction, Thriller");
        movie.setKeywords("dreams, reality");

        MovieResponse response = MovieMapper.toResponse(movie);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getGenres()).containsExactly("Science Fiction", "Thriller");
        assertThat(response.getKeywords()).containsExactly("dreams", "reality");
    }

    @Test
    void applyTrimsTitleAndJoinsLists() {
        MovieRequest request = new MovieRequest();
        request.setTitle("  Inception  ");
        request.setGenres(List.of("Science Fiction", " Thriller "));
        request.setKeywords(List.of("dreams"));
        request.setReleaseYear(2010);

        Movie movie = MovieMapper.toEntity(request);

        assertThat(movie.getTitle()).isEqualTo("Inception");
        assertThat(movie.getGenres()).isEqualTo("Science Fiction, Thriller");
        assertThat(movie.getKeywords()).isEqualTo("dreams");
        assertThat(movie.getReleaseYear()).isEqualTo(2010);
    }
}
