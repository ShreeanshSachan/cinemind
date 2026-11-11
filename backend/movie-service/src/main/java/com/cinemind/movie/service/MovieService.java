package com.cinemind.movie.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinemind.movie.dto.MovieFilter;
import com.cinemind.movie.dto.MovieMapper;
import com.cinemind.movie.dto.MovieRequest;
import com.cinemind.movie.dto.MovieResponse;
import com.cinemind.movie.dto.PagedResponse;
import com.cinemind.movie.entity.Movie;
import com.cinemind.movie.exception.DuplicateMovieException;
import com.cinemind.movie.exception.MovieNotFoundException;
import com.cinemind.movie.repository.MovieRepository;
import com.cinemind.movie.repository.MovieSpecifications;

/** All movie business rules. The controller only maps HTTP to this service. */
@Service
public class MovieService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "title", "releaseYear", "runtimeMinutes", "rating", "createdAt", "updatedAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<MovieResponse> list(int page, int size, String sortBy, String sortDir) {
        Page<Movie> result = movieRepository.findAll(pageable(page, size, sortBy, sortDir));
        return new PagedResponse<>(result.map(MovieMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public MovieResponse getById(Long id) {
        return MovieMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        String title = normalizedTitle(request);
        Integer year = request.getReleaseYear();
        rejectDuplicate(title, year, null);

        Movie saved = movieRepository.save(MovieMapper.toEntity(request));
        return MovieMapper.toResponse(saved);
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie existing = findOrThrow(id);
        String title = normalizedTitle(request);
        Integer year = request.getReleaseYear();
        rejectDuplicate(title, year, id);

        MovieMapper.apply(request, existing);
        return MovieMapper.toResponse(movieRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        movieRepository.delete(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<MovieResponse> search(String title, int page, int size,
                                               String sortBy, String sortDir) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'title' must not be blank");
        }
        Page<Movie> result = movieRepository.findByTitleContainingIgnoreCase(
                title.trim(), pageable(page, size, sortBy, sortDir));
        return new PagedResponse<>(result.map(MovieMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public PagedResponse<MovieResponse> filter(MovieFilter filter, int page, int size,
                                               String sortBy, String sortDir) {
        MovieFilter safeFilter = filter == null ? new MovieFilter() : filter;
        Page<Movie> result = movieRepository.findAll(
                MovieSpecifications.from(safeFilter), pageable(page, size, sortBy, sortDir));
        return new PagedResponse<>(result.map(MovieMapper::toResponse));
    }

    private Movie findOrThrow(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieNotFoundException(id));
    }

    private static String normalizedTitle(MovieRequest request) {
        // Bean Validation already guarantees a non-blank title.
        return request.getTitle().trim();
    }

    private void rejectDuplicate(String title, Integer releaseYear, Long ownId) {
        Optional<Movie> duplicate =
                movieRepository.findByTitleIgnoreCaseAndReleaseYear(title, releaseYear);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(ownId)) {
            throw new DuplicateMovieException(title, releaseYear);
        }
    }

    private static Pageable pageable(int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String property = SORTABLE_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }
}
