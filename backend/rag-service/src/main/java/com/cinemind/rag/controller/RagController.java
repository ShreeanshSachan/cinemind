package com.cinemind.rag.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cinemind.rag.dto.BulkIndexResult;
import com.cinemind.rag.dto.IndexResult;
import com.cinemind.rag.dto.RagHealthResponse;
import com.cinemind.rag.dto.RagSearchRequest;
import com.cinemind.rag.dto.RagSearchResponse;
import com.cinemind.rag.service.MovieIndexingService;
import com.cinemind.rag.service.MovieRetrievalService;
import com.cinemind.rag.service.RagHealthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final MovieIndexingService indexingService;
    private final MovieRetrievalService retrievalService;
    private final RagHealthService healthService;

    public RagController(MovieIndexingService indexingService,
                         MovieRetrievalService retrievalService,
                         RagHealthService healthService) {
        this.indexingService = indexingService;
        this.retrievalService = retrievalService;
        this.healthService = healthService;
    }

    @PostMapping("/index/{movieId}")
    public IndexResult indexMovie(@PathVariable long movieId,
                                  @RequestParam(defaultValue = "false") boolean force) {
        return indexingService.indexMovie(movieId, force);
    }

    @PostMapping("/index/all")
    public BulkIndexResult indexAll() {
        return indexingService.indexAll();
    }

    @PostMapping("/search")
    public RagSearchResponse search(@Valid @RequestBody RagSearchRequest request) {
        return retrievalService.search(request);
    }

    @GetMapping("/health")
    public RagHealthResponse health() {
        return healthService.health();
    }
}
