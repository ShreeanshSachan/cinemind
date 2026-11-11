package com.cinemind.rag.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cinemind.rag.client.MovieServiceClient;
import com.cinemind.rag.dto.MovieMetadata;
import com.cinemind.rag.dto.MovieMetadataFilter;
import com.cinemind.rag.dto.RagSearchRequest;
import com.cinemind.rag.dto.RagSearchResponse;
import com.cinemind.rag.dto.RetrievedMovie;
import com.cinemind.rag.entity.MovieEmbedding;
import com.cinemind.rag.repository.MovieEmbeddingRepository;
import com.cinemind.rag.util.CosineSimilarity;
import com.cinemind.rag.util.EmbeddingCodec;

/**
 * Retrieval: ONE query embedding, stored movie embeddings, pure Java cosine
 * similarity, deterministic metadata filtering and ranking. No LLM calls, no
 * vector database, and movie embeddings are never recomputed here.
 */
@Service
public class MovieRetrievalService {

    private final EmbeddingService embeddingService;
    private final MovieEmbeddingRepository repository;
    private final MovieServiceClient movieServiceClient;
    private final int defaultTopK;
    private final int maxTopK;

    public MovieRetrievalService(EmbeddingService embeddingService,
                                 MovieEmbeddingRepository repository,
                                 MovieServiceClient movieServiceClient,
                                 @Value("${cinemind.rag.default-top-k:10}") int defaultTopK,
                                 @Value("${cinemind.rag.max-top-k:50}") int maxTopK) {
        this.embeddingService = embeddingService;
        this.repository = repository;
        this.movieServiceClient = movieServiceClient;
        this.defaultTopK = defaultTopK;
        this.maxTopK = maxTopK;
    }

    public RagSearchResponse search(RagSearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        int topK = clampTopK(request.getTopK());
        double minScore = request.getMinScore() == null ? 0.0 : request.getMinScore();
        MovieMetadataFilter filters = request.getFilters() == null
                ? new MovieMetadataFilter()
                : request.getFilters();

        // Exactly one Ollama embedding call per query.
        float[] queryVector = embeddingService.embed(request.getQuery());

        // Current metadata is the single source of truth for hard filters and
        // for the metadata returned alongside each document.
        Map<Long, MovieMetadata> metadata = movieServiceClient.getAllMovies().stream()
                .filter(movie -> movie.getId() != null)
                .collect(Collectors.toMap(MovieMetadata::getId, Function.identity(), (a, b) -> a));

        Set<Long> allowedIds = metadata.values().stream()
                .filter(movie -> matches(movie, filters))
                .map(MovieMetadata::getId)
                .collect(Collectors.toSet());

        List<MovieEmbedding> candidates = allowedIds.isEmpty()
                ? List.of()
                : repository.findAllByMovieIdIn(allowedIds);

        List<RetrievedMovie> results = candidates.stream()
                .map(embedding -> new Scored(
                        embedding,
                        metadata.get(embedding.getMovieId()),
                        CosineSimilarity.cosine(queryVector,
                                EmbeddingCodec.decode(embedding.getEmbeddingJson()))))
                .filter(scored -> scored.score() >= minScore)
                // Deterministic: highest score first, then lowest movie id.
                .sorted(Comparator.comparingDouble(Scored::score).reversed()
                        .thenComparing(scored -> scored.embedding().getMovieId()))
                .limit(topK)
                .map(this::toResponse)
                .toList();

        return new RagSearchResponse(request.getQuery(), topK, candidates.size(),
                results.size(), filters, results);
    }

    private int clampTopK(Integer requested) {
        int value = requested == null ? defaultTopK : requested;
        return Math.min(Math.max(value, 1), maxTopK);
    }

    private static boolean matches(MovieMetadata movie, MovieMetadataFilter filter) {
        if (hasText(filter.getGenre()) && !anyGenreContains(movie.getGenres(), List.of(filter.getGenre()))) {
            return false;
        }
        if (!filter.getExcludedGenres().isEmpty()
                && anyGenreContains(movie.getGenres(), filter.getExcludedGenres())) {
            return false;
        }
        if (filter.getYearFrom() != null
                && (movie.getReleaseYear() == null || movie.getReleaseYear() < filter.getYearFrom())) {
            return false;
        }
        if (filter.getYearTo() != null
                && (movie.getReleaseYear() == null || movie.getReleaseYear() > filter.getYearTo())) {
            return false;
        }
        if (filter.getMinRuntime() != null
                && (movie.getRuntimeMinutes() == null || movie.getRuntimeMinutes() < filter.getMinRuntime())) {
            return false;
        }
        if (filter.getMaxRuntime() != null
                && (movie.getRuntimeMinutes() == null || movie.getRuntimeMinutes() > filter.getMaxRuntime())) {
            return false;
        }
        BigDecimal minRating = filter.getMinRating();
        if (minRating != null && (movie.getRating() == null || movie.getRating().compareTo(minRating) < 0)) {
            return false;
        }
        return true;
    }

    private static boolean anyGenreContains(List<String> genres, List<String> wanted) {
        if (genres == null || wanted == null) {
            return false;
        }
        return wanted.stream()
                .filter(MovieRetrievalService::hasText)
                .anyMatch(target -> genres.stream()
                        .filter(genre -> genre != null)
                        .anyMatch(genre -> genre.toLowerCase().contains(target.trim().toLowerCase())));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private RetrievedMovie toResponse(Scored scored) {
        MovieMetadata movie = scored.metadata();
        MovieEmbedding embedding = scored.embedding();
        double roundedScore = Math.round(scored.score() * 10000.0) / 10000.0;
        return new RetrievedMovie(
                embedding.getMovieId(),
                movie == null ? null : movie.getTitle(),
                movie == null ? null : movie.getOverview(),
                movie == null ? List.of() : movie.getGenres(),
                movie == null ? null : movie.getReleaseYear(),
                movie == null ? null : movie.getRuntimeMinutes(),
                movie == null ? null : movie.getRating(),
                movie == null ? null : movie.getPosterUrl(),
                embedding.getDocumentContent(),
                roundedScore,
                embedding.getModelName());
    }

    private record Scored(MovieEmbedding embedding, MovieMetadata metadata, double score) {
    }
}
