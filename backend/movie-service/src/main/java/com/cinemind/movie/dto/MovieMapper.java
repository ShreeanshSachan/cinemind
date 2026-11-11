package com.cinemind.movie.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.cinemind.movie.entity.Movie;

/**
 * Converts between the API DTOs and the entity. Genres and keywords are stored
 * as comma-separated text in MySQL and exposed as JSON arrays over HTTP.
 */
public final class MovieMapper {

    private MovieMapper() {
    }

    public static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    public static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    public static Movie toEntity(MovieRequest request) {
        Movie movie = new Movie();
        apply(request, movie);
        return movie;
    }

    public static void apply(MovieRequest request, Movie movie) {
        movie.setTitle(request.getTitle() == null ? null : request.getTitle().trim());
        movie.setOverview(request.getOverview());
        movie.setGenres(join(request.getGenres()));
        movie.setReleaseYear(request.getReleaseYear());
        movie.setRuntimeMinutes(request.getRuntimeMinutes());
        movie.setKeywords(join(request.getKeywords()));
        movie.setRating(request.getRating());
        movie.setPosterUrl(request.getPosterUrl());
    }

    public static MovieResponse toResponse(Movie movie) {
        MovieResponse response = new MovieResponse();
        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setOverview(movie.getOverview());
        response.setGenres(split(movie.getGenres()));
        response.setReleaseYear(movie.getReleaseYear());
        response.setRuntimeMinutes(movie.getRuntimeMinutes());
        response.setKeywords(split(movie.getKeywords()));
        response.setRating(movie.getRating());
        response.setPosterUrl(movie.getPosterUrl());
        response.setCreatedAt(movie.getCreatedAt());
        response.setUpdatedAt(movie.getUpdatedAt());
        return response;
    }
}
