package com.cinemind.rag.service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinemind.rag.client.MovieServiceClient;
import com.cinemind.rag.dto.MovieMetadata;
import com.cinemind.rag.dto.MovieMetadataFilter;
import com.cinemind.rag.dto.RagSearchRequest;
import com.cinemind.rag.dto.RagSearchResponse;
import com.cinemind.rag.entity.MovieEmbedding;
import com.cinemind.rag.repository.MovieEmbeddingRepository;
import com.cinemind.rag.util.EmbeddingCodec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieRetrievalServiceTest {

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private MovieEmbeddingRepository repository;
    @Mock
    private MovieServiceClient movieServiceClient;

    private MovieRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new MovieRetrievalService(
                embeddingService, repository, movieServiceClient, 10, 50);
    }

    @Test
    void ranksByCosineSimilarityWithOneQueryEmbedding() {
        MovieMetadata m1 = metadata(1L, "Best Match", List.of("Thriller"), 2010, 120);
        MovieMetadata m2 = metadata(2L, "Orthogonal", List.of("Comedy"), 2015, 100);
        MovieMetadata m3 = metadata(3L, "Partial Match", List.of("Drama"), 2012, 130);
        when(embeddingService.embed("mind bending thriller"))
                .thenReturn(new float[]{1f, 0f, 0f});
        when(movieServiceClient.getAllMovies()).thenReturn(List.of(m1, m2, m3));
        when(repository.findAllByMovieIdIn(ArgumentMatchers.<Long>anyCollection()))
                .thenReturn(List.of(
                        embedding(1L, "doc1", new float[]{0.9f, 0.1f, 0f}),
                        embedding(2L, "doc2", new float[]{0f, 1f, 0f}),
                        embedding(3L, "doc3", new float[]{1f, 1f, 0f})));

        RagSearchResponse response = retrievalService.search(request("mind bending thriller"));

        assertThat(response.topK()).isEqualTo(10);
        assertThat(response.candidateCount()).isEqualTo(3);
        assertThat(response.retrievedCount()).isEqualTo(3);
        assertThat(response.results()).extracting(r -> r.movieId())
                .containsExactly(1L, 3L, 2L);
        assertThat(response.results().get(0).similarityScore()).isGreaterThan(0.9);
        assertThat(response.results().get(0).title()).isEqualTo("Best Match");
        assertThat(response.results().get(0).documentContent()).isEqualTo("doc1");
        assertThat(response.results().get(0).modelName()).isEqualTo("nomic-embed-text");
        verify(embeddingService, times(1)).embed("mind bending thriller");
    }

    @Test
    void appliesMetadataFiltersBeforeRetrieval() {
        MovieMetadata m1 = metadata(1L, "Allowed", List.of("Mystery", "Thriller"), 2010, 138);
        MovieMetadata m2 = metadata(2L, "Wrong genre", List.of("Comedy"), 2015, 100);
        MovieMetadata m3 = metadata(3L, "Too long", List.of("Thriller"), 2012, 160);

        MovieMetadataFilter filter = new MovieMetadataFilter();
        filter.setGenre("Thriller");
        filter.setYearFrom(2010);
        filter.setYearTo(2013);
        filter.setMaxRuntime(140);

        when(embeddingService.embed(anyString())).thenReturn(new float[]{1f, 0f, 0f});
        when(movieServiceClient.getAllMovies()).thenReturn(List.of(m1, m2, m3));
        when(repository.findAllByMovieIdIn(ArgumentMatchers.<Long>anyCollection()))
                .thenReturn(List.of(embedding(1L, "doc1", new float[]{1f, 0f, 0f})));

        RagSearchRequest request = request("thriller");
        request.setFilters(filter);
        RagSearchResponse response = retrievalService.search(request);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findAllByMovieIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L);
        assertThat(response.appliedFilters()).isSameAs(filter);
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void excludedGenresAreRemovedFromCandidates() {
        MovieMetadata m1 = metadata(1L, "Horror", List.of("Horror", "Thriller"), 2010, 120);
        MovieMetadata m2 = metadata(2L, "Drama", List.of("Drama"), 2010, 120);

        MovieMetadataFilter filter = new MovieMetadataFilter();
        filter.setExcludedGenres(List.of("Horror"));

        when(embeddingService.embed(anyString())).thenReturn(new float[]{1f, 0f, 0f});
        when(movieServiceClient.getAllMovies()).thenReturn(List.of(m1, m2));
        when(repository.findAllByMovieIdIn(ArgumentMatchers.<Long>anyCollection()))
                .thenReturn(List.of(embedding(2L, "doc2", new float[]{1f, 0f, 0f})));

        RagSearchRequest request = request("anything");
        request.setFilters(filter);
        retrievalService.search(request);

        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findAllByMovieIdIn(captor.capture());
        assertThat(captor.getValue()).containsExactly(2L);
    }

    @Test
    void minScoreDropsWeakMatches() {
        when(embeddingService.embed(anyString())).thenReturn(new float[]{1f, 0f, 0f});
        when(movieServiceClient.getAllMovies())
                .thenReturn(List.of(metadata(1L, "A", List.of("Drama"), 2010, 120)));
        when(repository.findAllByMovieIdIn(ArgumentMatchers.<Long>anyCollection()))
                .thenReturn(List.of(embedding(1L, "doc", new float[]{0f, 1f, 0f})));

        RagSearchRequest request = request("query");
        request.setMinScore(0.5);
        RagSearchResponse response = retrievalService.search(request);

        assertThat(response.results()).isEmpty();
        assertThat(response.candidateCount()).isEqualTo(1);
    }

    @Test
    void topKIsHonouredAndResponseIncludesIt() {
        List<MovieMetadata> movies = List.of(
                metadata(1L, "A", List.of("Drama"), 2010, 120),
                metadata(2L, "B", List.of("Drama"), 2011, 121),
                metadata(3L, "C", List.of("Drama"), 2012, 122));
        when(embeddingService.embed(anyString())).thenReturn(new float[]{1f, 0f, 0f});
        when(movieServiceClient.getAllMovies()).thenReturn(movies);
        when(repository.findAllByMovieIdIn(ArgumentMatchers.<Long>anyCollection()))
                .thenReturn(List.of(
                        embedding(1L, "d1", new float[]{1f, 0f, 0f}),
                        embedding(2L, "d2", new float[]{1f, 0f, 0f}),
                        embedding(3L, "d3", new float[]{1f, 0f, 0f})));

        RagSearchRequest request = request("query");
        request.setTopK(2);
        RagSearchResponse response = retrievalService.search(request);

        assertThat(response.topK()).isEqualTo(2);
        assertThat(response.results()).hasSize(2);
        // Equal scores are broken deterministically by the lowest movie id.
        assertThat(response.results()).extracting(r -> r.movieId()).containsExactly(1L, 2L);
    }

    @Test
    void blankQueryIsRejectedWithoutCallingOllama() {
        assertThatThrownBy(() -> retrievalService.search(request("  ")))
                .isInstanceOf(IllegalArgumentException.class);
        verify(embeddingService, never()).embed(anyString());
    }

    private static RagSearchRequest request(String query) {
        RagSearchRequest request = new RagSearchRequest();
        request.setQuery(query);
        return request;
    }

    private static MovieMetadata metadata(long id, String title, List<String> genres,
                                          int year, int runtime) {
        MovieMetadata movie = new MovieMetadata();
        movie.setId(id);
        movie.setTitle(title);
        movie.setGenres(genres);
        movie.setReleaseYear(year);
        movie.setRuntimeMinutes(runtime);
        movie.setRating(new BigDecimal("8.0"));
        movie.setOverview("Overview of " + title);
        return movie;
    }

    private static MovieEmbedding embedding(long movieId, String document, float[] vector) {
        MovieEmbedding entity = new MovieEmbedding();
        entity.setId(movieId);
        entity.setMovieId(movieId);
        entity.setDocumentContent(document);
        entity.setEmbeddingJson(EmbeddingCodec.encode(vector));
        entity.setModelName("nomic-embed-text");
        return entity;
    }
}
