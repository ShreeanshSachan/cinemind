package com.cinemind.rag.service;

import org.springframework.stereotype.Service;

import com.cinemind.rag.dto.RagHealthResponse;
import com.cinemind.rag.repository.MovieEmbeddingRepository;

/** Builds the /api/rag/health response without ever throwing on Ollama errors. */
@Service
public class RagHealthService {

    private final OllamaStatusService ollamaStatusService;
    private final MovieEmbeddingRepository repository;
    private final EmbeddingService embeddingService;

    public RagHealthService(OllamaStatusService ollamaStatusService,
                            MovieEmbeddingRepository repository,
                            EmbeddingService embeddingService) {
        this.ollamaStatusService = ollamaStatusService;
        this.repository = repository;
        this.embeddingService = embeddingService;
    }

    public RagHealthResponse health() {
        OllamaStatusService.OllamaStatus ollama = ollamaStatusService.check();
        boolean healthy = ollama.available() && ollama.modelAvailable();
        return new RagHealthResponse(
                healthy ? "UP" : "DEGRADED",
                ollama.available(),
                ollama.modelAvailable(),
                embeddingService.modelName(),
                embeddingService.dimensions(),
                repository.count(),
                ollama.message());
    }
}
