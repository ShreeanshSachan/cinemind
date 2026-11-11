package com.cinemind.orchestrator.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cinemind.orchestrator.agent.PreferenceAgent;
import com.cinemind.orchestrator.agent.RecommendationAgent;
import com.cinemind.orchestrator.client.RagServiceClient;
import com.cinemind.orchestrator.dto.AgentTrace;
import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.RecommendationRequest;
import com.cinemind.orchestrator.dto.RecommendationResponse;
import com.cinemind.orchestrator.dto.RecommendationResult;
import com.cinemind.orchestrator.dto.RecommendedMovie;
import com.cinemind.orchestrator.dto.rag.RagFilters;
import com.cinemind.orchestrator.dto.rag.RagResult;
import com.cinemind.orchestrator.dto.rag.RagSearchRequest;
import com.cinemind.orchestrator.dto.rag.RagSearchResponse;

/**
 * Deterministic 9-step workflow. No autonomous loops, no agent-to-agent
 * recursion and exactly one Gemini call per agent (two per normal request).
 */
@Service
public class RecommendationOrchestrator {

    private final PreferenceAgent preferenceAgent;
    private final RagServiceClient ragServiceClient;
    private final RecommendationAgent recommendationAgent;
    private final PreferenceValidator preferenceValidator;
    private final HardFilterService hardFilterService;
    private final GeminiAvailability geminiAvailability;
    private final int ragTopK;
    private final int maxRecommendations;

    public RecommendationOrchestrator(
            PreferenceAgent preferenceAgent,
            RagServiceClient ragServiceClient,
            RecommendationAgent recommendationAgent,
            PreferenceValidator preferenceValidator,
            HardFilterService hardFilterService,
            GeminiAvailability geminiAvailability,
            @Value("${cinemind.orchestrator.rag-top-k:10}") int ragTopK,
            @Value("${cinemind.orchestrator.max-recommendations:5}") int maxRecommendations) {
        this.preferenceAgent = preferenceAgent;
        this.ragServiceClient = ragServiceClient;
        this.recommendationAgent = recommendationAgent;
        this.preferenceValidator = preferenceValidator;
        this.hardFilterService = hardFilterService;
        this.geminiAvailability = geminiAvailability;
        this.ragTopK = ragTopK;
        this.maxRecommendations = maxRecommendations;
    }

    public RecommendationResult recommend(RecommendationRequest request) {
        // Fail fast on missing credentials: no Gemini call, no wasted credits.
        geminiAvailability.requireConfigured();

        // 1. Receive the user query.
        String query = request.getQuery();
        int maxResults = clampMaxResults(request.getMaxResults());

        // 2. Invoke Preference Analyst (one Gemini call).
        MoviePreferences rawPreferences = preferenceAgent.analyze(query);

        // 3. Validate structured preferences in Java.
        MoviePreferences preferences = preferenceValidator.validateAndNormalize(rawPreferences);

        // 4. Call RAG Service and 5. retrieve the top 10 documents.
        RagSearchResponse ragResponse = ragServiceClient.search(buildRagRequest(query, preferences));
        List<RagResult> retrieved = ragResponse.getResults() == null
                ? List.of()
                : ragResponse.getResults();
        int retrievedDocuments = retrieved.size();

        // 6. Apply hard metadata filters in Java.
        List<RagResult> candidates = hardFilterService.apply(retrieved, preferences);

        // 7. Invoke Recommendation Specialist (one Gemini call).
        RecommendationResponse agentResponse =
                recommendationAgent.recommend(preferences, candidates, maxResults);

        // 8. Build the final recommendations, using only retrieved metadata.
        List<RecommendedMovie> recommendations = finalize(agentResponse, candidates, maxResults);

        // 9. Execution trace from the real workflow, not hardcoded values.
        AgentTrace trace = new AgentTrace("COMPLETED", "COMPLETED", "COMPLETED",
                retrievedDocuments, candidates.size());

        return new RecommendationResult(query, maxResults, retrievedDocuments,
                preferences, recommendations, trace);
    }

    private RagSearchRequest buildRagRequest(String query, MoviePreferences preferences) {
        RagSearchRequest ragRequest = new RagSearchRequest();
        ragRequest.setQuery(query);
        ragRequest.setTopK(ragTopK);

        // Only unambiguous numeric/negative constraints are pushed down to RAG.
        // Fuzzy genre intent is left to semantic similarity, then re-checked in
        // Java by HardFilterService - no LLM reasoning for simple constraints.
        RagFilters filters = new RagFilters();
        filters.setYearFrom(preferences.getReleaseYearMin());
        filters.setMaxRuntime(preferences.getMaxRuntime());
        filters.setExcludedGenres(preferences.getExcludedGenres());
        ragRequest.setFilters(filters);
        return ragRequest;
    }

    private List<RecommendedMovie> finalize(RecommendationResponse agentResponse,
                                            List<RagResult> candidates,
                                            int maxResults) {
        Map<Long, RagResult> byId = new HashMap<>();
        for (RagResult candidate : candidates) {
            byId.putIfAbsent(candidate.getMovieId(), candidate);
        }

        List<RecommendedMovie> recommendations = new ArrayList<>();
        if (agentResponse == null || agentResponse.getRecommendations() == null) {
            return recommendations;
        }

        for (RecommendationResponse.RecommendationItem item : agentResponse.getRecommendations()) {
            if (recommendations.size() >= maxResults) {
                break;
            }
            if (item.getMovieId() == null) {
                continue;
            }
            RagResult candidate = byId.get(item.getMovieId());
            if (candidate == null) {
                // Agent invented an ID that is not in the retrieved context: drop it.
                continue;
            }
            recommendations.add(new RecommendedMovie(
                    candidate.getMovieId(),
                    candidate.getTitle(),       // real metadata, never LLM text
                    candidate.getReleaseYear(), // real metadata, never LLM text
                    clampScore(item.getMatchScore()),
                    sanitizeReason(item.getReason())));
        }
        return recommendations;
    }

    private int clampMaxResults(Integer requested) {
        int value = requested == null ? maxRecommendations : requested;
        return Math.min(Math.max(value, 1), maxRecommendations);
    }

    private static int clampScore(Integer score) {
        return score == null ? 0 : Math.min(Math.max(score, 0), 100);
    }

    private static String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Matches the requested preferences.";
        }
        String trimmed = reason.trim();
        return trimmed.substring(0, Math.min(trimmed.length(), 300));
    }
}
