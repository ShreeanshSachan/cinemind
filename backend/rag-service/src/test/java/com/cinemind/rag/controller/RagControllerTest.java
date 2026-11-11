package com.cinemind.rag.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinemind.rag.dto.BulkIndexResult;
import com.cinemind.rag.dto.IndexResult;
import com.cinemind.rag.dto.IndexStatus;
import com.cinemind.rag.dto.MovieMetadataFilter;
import com.cinemind.rag.dto.RagHealthResponse;
import com.cinemind.rag.dto.RagSearchResponse;
import com.cinemind.rag.dto.RetrievedMovie;
import com.cinemind.rag.exception.EmbeddingUnavailableException;
import com.cinemind.rag.service.MovieIndexingService;
import com.cinemind.rag.service.MovieRetrievalService;
import com.cinemind.rag.service.RagHealthService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieIndexingService indexingService;
    @MockitoBean
    private MovieRetrievalService retrievalService;
    @MockitoBean
    private RagHealthService healthService;

    @Test
    void indexSingleMovie() throws Exception {
        when(indexingService.indexMovie(17L, false))
                .thenReturn(new IndexResult(17L, "Shutter Island", IndexStatus.CREATED,
                        768, "Embedding stored in MySQL"));

        mockMvc.perform(post("/api/rag/index/17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movieId").value(17))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.dimensions").value(768));

        verify(indexingService).indexMovie(17L, false);
    }

    @Test
    void indexSingleMovieWithForce() throws Exception {
        when(indexingService.indexMovie(17L, true))
                .thenReturn(new IndexResult(17L, "Shutter Island", IndexStatus.UPDATED,
                        768, "Embedding stored in MySQL"));

        mockMvc.perform(post("/api/rag/index/17").param("force", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPDATED"));

        verify(indexingService).indexMovie(17L, true);
    }

    @Test
    void indexAll() throws Exception {
        when(indexingService.indexAll())
                .thenReturn(new BulkIndexResult(26, 26, 0, 0, "nomic-embed-text"));

        mockMvc.perform(post("/api/rag/index/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMovies").value(26))
                .andExpect(jsonPath("$.created").value(26))
                .andExpect(jsonPath("$.modelName").value("nomic-embed-text"));
    }

    @Test
    void searchReturnsRankedResults() throws Exception {
        RagSearchResponse response = new RagSearchResponse(
                "dark thriller", 10, 1, 1, new MovieMetadataFilter(),
                List.of(new RetrievedMovie(17L, "Shutter Island", "Overview",
                        List.of("Thriller"), 2010, 138, null, null,
                        "doc", 0.9123, "nomic-embed-text")));
        when(retrievalService.search(any())).thenReturn(response);

        mockMvc.perform(post("/api/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"dark thriller\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("dark thriller"))
                .andExpect(jsonPath("$.topK").value(10))
                .andExpect(jsonPath("$.results[0].movieId").value(17))
                .andExpect(jsonPath("$.results[0].similarityScore").value(0.9123));
    }

    @Test
    void searchRejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.query").exists());
    }

    @Test
    void searchReportsOllamaFailureAs503() throws Exception {
        when(retrievalService.search(any())).thenThrow(
                new EmbeddingUnavailableException("Ollama embedding failed for model 'nomic-embed-text'."));

        mockMvc.perform(post("/api/rag/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"dark thriller\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "Ollama embedding failed for model 'nomic-embed-text'."));
    }

    @Test
    void healthEndpoint() throws Exception {
        when(healthService.health()).thenReturn(new RagHealthResponse(
                "UP", true, true, "nomic-embed-text", 768, 26,
                "Ollama is reachable and model 'nomic-embed-text' is available."));

        mockMvc.perform(get("/api/rag/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.ollamaAvailable").value(true))
                .andExpect(jsonPath("$.embeddingDimension").value(768))
                .andExpect(jsonPath("$.indexedMovies").value(26));
    }

    @Test
    void invalidMovieIdReturns400() throws Exception {
        mockMvc.perform(post("/api/rag/index/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
