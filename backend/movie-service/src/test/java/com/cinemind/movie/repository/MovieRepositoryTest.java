package com.cinemind.movie.repository;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.cinemind.movie.dto.MovieFilter;
import com.cinemind.movie.entity.Movie;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void savesAndFindsByTitleIgnoringCase() {
        movieRepository.save(movie("Inception", 2010, "Science Fiction,Thriller", 148, "8.8"));

        Page<Movie> result = movieRepository.findByTitleContainingIgnoreCase(
                "CEPT", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Inception");
    }

    @Test
    void findsDuplicateByTitleAndYearIgnoringCase() {
        movieRepository.save(movie("Inception", 2010, "Science Fiction,Thriller", 148, "8.8"));

        assertThat(movieRepository.findByTitleIgnoreCaseAndReleaseYear("inception", 2010))
                .isPresent();
        assertThat(movieRepository.findByTitleIgnoreCaseAndReleaseYear("Inception", 2011))
                .isEmpty();
    }

    @Test
    void timestampsArePopulatedOnPersist() {
        Movie saved = movieRepository.save(movie("Memento", 2000, "Mystery,Thriller", 113, "8.4"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void specificationFiltersByGenreYearAndRuntime() {
        movieRepository.save(movie("Shutter Island", 2010, "Mystery,Thriller", 138, "8.2"));
        movieRepository.save(movie("Inception", 2010, "Science Fiction,Thriller", 148, "8.8"));
        movieRepository.save(movie("Prisoners", 2013, "Thriller,Crime", 153, "8.1"));
        movieRepository.save(movie("Arrival", 2016, "Science Fiction,Drama", 116, "7.9"));

        MovieFilter filter = new MovieFilter();
        filter.setGenre("Thriller");
        filter.setYearFrom(2010);
        filter.setYearTo(2010);
        filter.setMaxRuntime(140);

        Page<Movie> result = movieRepository.findAll(
                MovieSpecifications.from(filter), PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Movie::getTitle)
                .containsExactly("Shutter Island");
    }

    @Test
    void specificationWithNoFiltersReturnsEverything() {
        movieRepository.save(movie("Memento", 2000, "Mystery,Thriller", 113, "8.4"));
        movieRepository.save(movie("Arrival", 2016, "Science Fiction,Drama", 116, "7.9"));

        Page<Movie> result = movieRepository.findAll(
                MovieSpecifications.from(new MovieFilter()), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    private static Movie movie(String title, int year, String genres, int runtime, String rating) {
        Movie movie = new Movie();
        movie.setTitle(title);
        movie.setReleaseYear(year);
        movie.setGenres(genres);
        movie.setRuntimeMinutes(runtime);
        movie.setRating(new BigDecimal(rating));
        movie.setOverview("Overview of " + title);
        movie.setKeywords("keyword");
        return movie;
    }
}
