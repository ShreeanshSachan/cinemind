package com.cinemind.rag.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cinemind.rag.entity.MovieEmbedding;

public interface MovieEmbeddingRepository extends JpaRepository<MovieEmbedding, Long> {

    /** Used to avoid duplicate indexing and to re-index an updated movie. */
    Optional<MovieEmbedding> findByMovieId(Long movieId);

    boolean existsByMovieId(Long movieId);

    /** Restricts the candidate set after deterministic metadata filtering. */
    List<MovieEmbedding> findAllByMovieIdIn(Collection<Long> movieIds);

    List<MovieEmbedding> findAllByModelName(String modelName);
}
