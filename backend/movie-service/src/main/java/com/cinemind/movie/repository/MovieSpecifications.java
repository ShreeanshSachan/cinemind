package com.cinemind.movie.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.cinemind.movie.dto.MovieFilter;
import com.cinemind.movie.entity.Movie;

import jakarta.persistence.criteria.Predicate;

/** Builds a dynamic WHERE clause for GET /api/movies/filter. */
public final class MovieSpecifications {

    private MovieSpecifications() {
    }

    public static Specification<Movie> from(MovieFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(filter.getGenre())) {
                String pattern = "%" + filter.getGenre().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.<String>get("genres")), pattern));
            }
            if (filter.getYearFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("releaseYear"), filter.getYearFrom()));
            }
            if (filter.getYearTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("releaseYear"), filter.getYearTo()));
            }
            if (filter.getMinRuntime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("runtimeMinutes"), filter.getMinRuntime()));
            }
            if (filter.getMaxRuntime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("runtimeMinutes"), filter.getMaxRuntime()));
            }
            if (filter.getMinRating() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<BigDecimal>get("rating"), filter.getMinRating()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
