package com.cinemind.movie.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.cinemind.movie.dto.MovieFilter;
import com.cinemind.movie.dto.MovieRequest;
import com.cinemind.movie.dto.MovieResponse;
import com.cinemind.movie.dto.PagedResponse;
import com.cinemind.movie.entity.Movie;
import com.cinemind.movie.exception.DuplicateMovieException;
import com.cinemind.movie.exception.MovieNotFoundException;
import com.cinemind.movie.repository.MovieRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository);
    }

    @Test
    void createPersistsAndReturnsResponse() {
        when(movieRepository.findByTitleIgnoreCaseAndReleaseYear("Inception", 2010))
                .thenReturn(Optional.empty());
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        MovieResponse response = movieService.create(request("Inception", 2010));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Inception");
        assertThat(response.getGenres()).containsExactly("Science Fiction", "Thriller");
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void createRejectsDuplicateTitleAndYear() {
        when(movieRepository.findByTitleIgnoreCaseAndReleaseYear("Inception", 2010))
                .thenReturn(Optional.of(entity(1L, "Inception", 2010)));

        assertThatThrownBy(() -> movieService.create(request("Inception", 2010)))
                .isInstanceOf(DuplicateMovieException.class);
        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.getById(99L))
                .isInstanceOf(MovieNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateAllowsKeepingTheSameTitle() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(entity(1L, "Inception", 2010)));
        when(movieRepository.findByTitleIgnoreCaseAndReleaseYear("Inception", 2010))
                .thenReturn(Optional.of(entity(1L, "Inception", 2010)));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieResponse response = movieService.update(1L, request("Inception", 2010));

        assertThat(response.getTitle()).isEqualTo("Inception");
    }

    @Test
    void updateRejectsCollidingWithAnotherMovie() {
        when(movieRepository.findById(1L)).thenReturn(Optional.of(entity(1L, "Inception", 2010)));
        when(movieRepository.findByTitleIgnoreCaseAndReleaseYear("Inception", 2010))
                .thenReturn(Optional.of(entity(2L, "Inception", 2010)));

        assertThatThrownBy(() -> movieService.update(1L, request("Inception", 2010)))
                .isInstanceOf(DuplicateMovieException.class);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(movieRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.delete(42L))
                .isInstanceOf(MovieNotFoundException.class);
        verify(movieRepository, never()).delete(any(Movie.class));
    }

    @Test
    void searchRejectsBlankTitle() {
        assertThatThrownBy(() -> movieService.search("  ", 0, 20, "id", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchDelegatesToRepository() {
        Page<Movie> page = new PageImpl<>(List.of(entity(1L, "Inception", 2010)));
        when(movieRepository.findByTitleContainingIgnoreCase(any(String.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<MovieResponse> result = movieService.search("ception", 0, 20, "id", "asc");

        assertThat(result.getContent()).hasSize(1);
        verify(movieRepository).findByTitleContainingIgnoreCase("ception", PageRequest.of(0, 20,
                Sort.by(Sort.Direction.ASC, "id")));
    }

    @Test
    void listClampsPageSizeAndWhitelistsSortField() {
        when(movieRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        movieService.list(0, 500, "not-a-field", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(movieRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void filterUsesSpecification() {
        when(movieRepository.findAll(
                ArgumentMatchers.<Specification<Movie>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        MovieFilter filter = new MovieFilter();
        filter.setGenre("Thriller");
        filter.setYearFrom(2010);
        filter.setMaxRuntime(140);

        PagedResponse<MovieResponse> result = movieService.filter(filter, 0, 10, "id", "asc");

        assertThat(result.getContent()).isEmpty();
        verify(movieRepository).findAll(
                ArgumentMatchers.<Specification<Movie>>any(), any(Pageable.class));
    }

    private static MovieRequest request(String title, Integer year) {
        MovieRequest request = new MovieRequest();
        request.setTitle(title);
        request.setReleaseYear(year);
        request.setGenres(List.of("Science Fiction", "Thriller"));
        request.setKeywords(List.of("dreams"));
        request.setRuntimeMinutes(148);
        request.setRating(new BigDecimal("8.8"));
        return request;
    }

    private static Movie entity(Long id, String title, Integer year) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setTitle(title);
        movie.setReleaseYear(year);
        movie.setGenres("Science Fiction, Thriller");
        movie.setRuntimeMinutes(148);
        movie.setRating(new BigDecimal("8.8"));
        return movie;
    }
}
