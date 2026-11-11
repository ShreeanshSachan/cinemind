package com.cinemind.movie.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.cinemind.movie.entity.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    /** Used to detect duplicates on create and update (title + year is unique). */
    Optional<Movie> findByTitleIgnoreCaseAndReleaseYear(String title, Integer releaseYear);
}
