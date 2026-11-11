package com.cinemind.rag.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cinemind.rag.client.MovieServiceClient;
import com.cinemind.rag.dto.BulkIndexResult;
import com.cinemind.rag.dto.IndexResult;
import com.cinemind.rag.dto.IndexStatus;
import com.cinemind.rag.dto.MovieMetadata;
import com.cinemind.rag.entity.MovieEmbedding;
import com.cinemind.rag.exception.MovieNotFoundException;
import com.cinemind.rag.repository.MovieEmbeddingRepository;
import com.cinemind.rag.util.EmbeddingCodec;

/**
 * Ingestion only. Embeddings are generated for NEW or UPDATED movies and never
 * for a search request. Re-running is safe because movie_id is unique and an
 * unchanged document is skipped.
 */
@Service
public class MovieIndexingService {

    private final MovieServiceClient movieServiceClient;
    private final MovieDocumentService documentService;
    private final EmbeddingService embeddingService;
    private final MovieEmbeddingRepository repository;

    public MovieIndexingService(MovieServiceClient movieServiceClient,
                                MovieDocumentService documentService,
                                EmbeddingService embeddingService,
                                MovieEmbeddingRepository repository) {
        this.movieServiceClient = movieServiceClient;
        this.documentService = documentService;
        this.embeddingService = embeddingService;
        this.repository = repository;
    }

    @Transactional
    public IndexResult indexMovie(long movieId, boolean force) {
        MovieMetadata movie = movieServiceClient.getMovie(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));
        return index(movie, force);
    }

    @Transactional
    public BulkIndexResult indexAll() {
        List<MovieMetadata> movies = movieServiceClient.getAllMovies();
        int created = 0;
        int updated = 0;
        int skipped = 0;

        for (MovieMetadata movie : movies) {
            if (movie.getId() == null) {
                continue;
            }
            IndexStatus status = index(movie, false).status();
            switch (status) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case SKIPPED -> skipped++;
            }
        }
        return new BulkIndexResult(movies.size(), created, updated, skipped,
                embeddingService.modelName());
    }

    private IndexResult index(MovieMetadata movie, boolean force) {
        String document = documentService.buildDocument(movie);
        Optional<MovieEmbedding> existing = repository.findByMovieId(movie.getId());

        // Avoid duplicate indexing and avoid regenerating an unchanged vector.
        // A changed document (edited metadata) or a changed model triggers a
        // genuine re-index; force=true always re-embeds.
        boolean unchanged = existing.isPresent()
                && Objects.equals(existing.get().getDocumentContent(), document)
                && Objects.equals(existing.get().getModelName(), embeddingService.modelName());
        if (unchanged && !force) {
            return new IndexResult(movie.getId(), movie.getTitle(), IndexStatus.SKIPPED,
                    embeddingService.dimensions(), "Already indexed with this document and model");
        }

        float[] vector = embeddingService.embed(document);

        MovieEmbedding entity = existing.orElseGet(MovieEmbedding::new);
        entity.setMovieId(movie.getId());
        entity.setDocumentContent(document);
        entity.setEmbeddingJson(EmbeddingCodec.encode(vector));
        entity.setModelName(embeddingService.modelName());
        repository.save(entity);

        IndexStatus status = existing.isPresent() ? IndexStatus.UPDATED : IndexStatus.CREATED;
        return new IndexResult(movie.getId(), movie.getTitle(), status,
                vector.length, "Embedding stored in MySQL");
    }
}
