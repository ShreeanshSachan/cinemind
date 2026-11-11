package com.cinemind.rag.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Non-invasive Ollama probe using GET /api/tags. It does not generate any
 * embedding, so a health check costs no model inference. Both an unreachable
 * Ollama and a missing model are reported gracefully instead of failing.
 */
@Service
public class OllamaStatusService {

    private final RestClient restClient;
    private final String baseUrl;
    private final String modelName;

    public OllamaStatusService(
            @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}") String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public OllamaStatus check() {
        try {
            JsonNode root = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(JsonNode.class);
            boolean present = isModelPresent(root, modelName);
            String message = present
                    ? "Ollama is reachable and model '" + modelName + "' is available."
                    : "Ollama is reachable but model '" + modelName
                            + "' is not pulled. Run: ollama pull " + modelName;
            return new OllamaStatus(true, present, message);
        } catch (Exception ex) {
            return new OllamaStatus(false, false,
                    "Ollama is unreachable at " + baseUrl + " (" + ex.getClass().getSimpleName()
                            + "). Install/start Ollama and pull " + modelName + ".");
        }
    }

    /** Package-visible for unit tests; handles the "name:tag" Ollama convention. */
    static boolean isModelPresent(JsonNode root, String model) {
        if (root == null || model == null || model.isBlank()) {
            return false;
        }
        JsonNode models = root.path("models");
        if (!models.isArray()) {
            return false;
        }
        String wanted = model.trim();
        for (JsonNode entry : models) {
            String name = entry.path("name").asText("");
            if (name.equals(wanted) || name.startsWith(wanted + ":")) {
                return true;
            }
        }
        return false;
    }

    public record OllamaStatus(boolean available, boolean modelAvailable, String message) {
    }
}
