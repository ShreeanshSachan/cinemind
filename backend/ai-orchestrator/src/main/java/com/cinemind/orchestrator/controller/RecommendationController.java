package com.cinemind.orchestrator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cinemind.orchestrator.dto.HealthResponse;
import com.cinemind.orchestrator.dto.RecommendationRequest;
import com.cinemind.orchestrator.dto.RecommendationResult;
import com.cinemind.orchestrator.service.GeminiAvailability;
import com.cinemind.orchestrator.service.RecommendationOrchestrator;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationOrchestrator orchestrator;
    private final GeminiAvailability geminiAvailability;
    private final String llmModel;

    public RecommendationController(
            RecommendationOrchestrator orchestrator,
            GeminiAvailability geminiAvailability,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}") String llmModel) {
        this.orchestrator = orchestrator;
        this.geminiAvailability = geminiAvailability;
        this.llmModel = llmModel;
    }

    @PostMapping
    public RecommendationResult recommend(@Valid @RequestBody RecommendationRequest request) {
        return orchestrator.recommend(request);
    }

    @GetMapping("/health")
    public HealthResponse health() {
        boolean configured = geminiAvailability.isConfigured();
        return new HealthResponse(configured ? "UP" : "DEGRADED",
                llmModel, configured, 2);
    }
}
