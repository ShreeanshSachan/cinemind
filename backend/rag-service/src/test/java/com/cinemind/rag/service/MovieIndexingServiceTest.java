package com.cinemind.rag.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinemind.rag.client.MovieServiceClient;
import com.cinemind.rag.dto.BulkIndexResult;
import com.cinemind.rag.dto.IndexResult;
import com.cinemind.rag.dto.IndexStatus;
import com.cinemind.rag.dto.MovieMetadata;
import com.cinemind.rag.entity.MovieEmbedding;
import com.cinemind.rag.exception.MovieNotFoundException;
import com.cinemind.rag.repository.MovieEmbeddingRepository;
import com.cinemind.rag.util.EmbeddingCodec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieIndexingServiceTest {

    @Mock
    private MovieServiceClient movieServiceClient;
    @Mock
    private MovieDocumentService documentService;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private MovieEmbeddingRepository repository;

    private MovieIndexingService indexingService;

    @BeforeEach
    void setUp() {
        indexingService = new MovieIndexingService(
                movieServiceClient, documentService, embeddingService, repository);
    }

    @Test
    void indexesANewMovieAndPersistsTheVector() {
        MovieMetadata movie = movie(17L);
        when(movieServiceClient.getMovie(17L)).thenReturn(Optional.of(movie));
        when(documentService.buildDocument(movie)).thenReturn("doc");
        when(repository.findByMovieId(17L)).thenReturn(Optional.empty());
        when(embeddingService.embed("doc")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});
        when(embeddingService.modelName()).thenReturn("nomic-embed-text");
        when(repository.save(any(MovieEmbedding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IndexResult result = indexingService.indexMovie(17L, false);

        assertThat(result.status()).isEqualTo(IndexStatus.CREATED);
        assertThat(result.movieId()).isEqualTo(17L);
        // Reports the length of the vector actually stored (768 with the real model).
        assertThat(result.dimensions()).isEqualTo(3);

        ArgumentCaptor<MovieEmbedding> captor = ArgumentCaptor.forClass(MovieEmbedding.class);
        verify(repository).save(captor.capture());
        MovieEmbedding saved = captor.getValue();
        assertThat(saved.getMovieId()).isEqualTo(17L);
        assertThat(saved.getDocumentContent()).isEqualTo("doc");
        assertThat(saved.getModelName()).isEqualTo("nomic-embed-text");
        assertThat(EmbeddingCodec.decode(saved.getEmbeddingJson()))
                .containsExactly(0.1f, 0.2f, 0.3f);
        verify(embeddingService).embed("doc");
    }

    @Test
    void skipsAnUnchangedMovieWithoutRegeneratingTheEmbedding() {
        MovieMetadata movie = movie(17L);
        MovieEmbedding existing = embedding(17L, "doc", "nomic-embed-text");
        when(movieServiceClient.getMovie(17L)).thenReturn(Optional.of(movie));
        when(documentService.buildDocument(movie)).thenReturn("doc");
        when(repository.findByMovieId(17L)).thenReturn(Optional.of(existing));
        when(embeddingService.modelName()).thenReturn("nomic-embed-text");
        when(embeddingService.dimensions()).thenReturn(768);

        IndexResult result = indexingService.indexMovie(17L, false);

        assertThat(result.status()).isEqualTo(IndexStatus.SKIPPED);
        verify(embeddingService, never()).embed(anyString());
        verify(repository, never()).save(any(MovieEmbedding.class));
    }

    @Test
    void reIndexesWhenTheDocumentChanged() {
        MovieMetadata movie = movie(17L);
        MovieEmbedding existing = embedding(17L, "old doc", "nomic-embed-text");
        when(movieServiceClient.getMovie(17L)).thenReturn(Optional.of(movie));
        when(documentService.buildDocument(movie)).thenReturn("doc");
        when(repository.findByMovieId(17L)).thenReturn(Optional.of(existing));
        when(embeddingService.modelName()).thenReturn("nomic-embed-text");
        when(embeddingService.embed("doc")).thenReturn(new float[]{1f, 0f, 0f});
        when(repository.save(any(MovieEmbedding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IndexResult result = indexingService.indexMovie(17L, false);

        assertThat(result.status()).isEqualTo(IndexStatus.UPDATED);
        assertThat(existing.getDocumentContent()).isEqualTo("doc");
        verify(embeddingService).embed("doc");
    }

    @Test
    void forceReIndexesAnUnchangedMovie() {
        MovieMetadata movie = movie(17L);
        MovieEmbedding existing = embedding(17L, "doc", "nomic-embed-text");
        when(movieServiceClient.getMovie(17L)).thenReturn(Optional.of(movie));
        when(documentService.buildDocument(movie)).thenReturn("doc");
        when(repository.findByMovieId(17L)).thenReturn(Optional.of(existing));
        when(embeddingService.modelName()).thenReturn("nomic-embed-text");
        when(embeddingService.embed("doc")).thenReturn(new float[]{1f, 0f, 0f});
        when(repository.save(any(MovieEmbedding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IndexResult result = indexingService.indexMovie(17L, true);

        assertThat(result.status()).isEqualTo(IndexStatus.UPDATED);
        verify(embeddingService).embed("doc");
    }

    @Test
    void throwsWhenMovieDoesNotExist() {
        when(movieServiceClient.getMovie(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> indexingService.indexMovie(99L, false))
                .isInstanceOf(MovieNotFoundException.class)
                .hasMessageContaining("99");
        verify(embeddingService, never()).embed(anyString());
    }

    @Test
    void indexAllCountsCreatedUpdatedAndSkipped() {
        MovieMetadata first = movie(1L);
        MovieMetadata second = movie(2L);
        MovieMetadata third = movie(3L);
        when(movieServiceClient.getAllMovies()).thenReturn(List.of(first, second, third));
        when(documentService.buildDocument(any(MovieMetadata.class))).thenReturn("doc");
        when(repository.findByMovieId(1L)).thenReturn(Optional.empty());
        when(repository.findByMovieId(2L))
                .thenReturn(Optional.of(embedding(2L, "doc", "nomic-embed-text")));
        when(repository.findByMovieId(3L))
                .thenReturn(Optional.of(embedding(3L, "stale", "nomic-embed-text")));
        when(embeddingService.modelName()).thenReturn("nomic-embed-text");
        when(embeddingService.dimensions()).thenReturn(768);
        when(embeddingService.embed(anyString())).thenReturn(new float[]{1f, 0f, 0f});
        when(repository.save(any(MovieEmbedding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BulkIndexResult result = indexingService.indexAll();

        assertThat(result.totalMovies()).isEqualTo(3);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.modelName()).isEqualTo("nomic-embed-text");
        // Only the new and the changed movie are embedded.
        verify(embeddingService, org.mockito.Mockito.times(2)).embed(anyString());
    }

    private static MovieMetadata movie(long id) {
        MovieMetadata movie = new MovieMetadata();
        movie.setId(id);
        movie.setTitle("Movie " + id);
        movie.setReleaseYear(2010);
        return movie;
    }

    private static MovieEmbedding embedding(long movieId, String document, String model) {
        MovieEmbedding entity = new MovieEmbedding();
        entity.setId(movieId);
        entity.setMovieId(movieId);
        entity.setDocumentContent(document);
        entity.setEmbeddingJson("[1.0,0.0,0.0]");
        entity.setModelName(model);
        return entity;
    }
}
