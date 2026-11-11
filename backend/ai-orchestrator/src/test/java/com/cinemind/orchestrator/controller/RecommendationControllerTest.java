package com.cinemind.orchestrator.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinemind.orchestrator.dto.AgentTrace;
import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.RecommendationResult;
import com.cinemind.orchestrator.dto.RecommendedMovie;
import com.cinemind.orchestrator.exception.LlmUnavailableException;
import com.cinemind.orchestrator.service.GeminiAvailability;
import com.cinemind.orchestrator.service.RecommendationOrchestrator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationOrchestrator orchestrator;

    @MockitoBean
    private GeminiAvailability geminiAvailability;

    @Test
    void postReturnsRecommendationsAndTrace() throws Exception {
        when(orchestrator.recommend(any())).thenReturn(new RecommendationResult(
                "dark thriller", 5, 10, new MoviePreferences(),
                List.of(new RecommendedMovie(17L, "Shutter Island", 2010, 95,
                        "Dark psychological tension")),
                new AgentTrace("COMPLETED", "COMPLETED", "COMPLETED", 10, 5)));

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"dark thriller\",\"maxResults\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("dark thriller"))
                .andExpect(jsonPath("$.retrievedDocuments").value(10))
                .andExpect(jsonPath("$.recommendations[0].movieId").value(17))
                .andExpect(jsonPath("$.recommendations[0].title").value("Shutter Island"))
                .andExpect(jsonPath("$.recommendations[0].releaseYear").value(2010))
                .andExpect(jsonPath("$.recommendations[0].matchScore").value(95))
                .andExpect(jsonPath("$.recommendations[0].reason")
                        .value("Dark psychological tension"))
                .andExpect(jsonPath("$.agentTrace.preferenceAgent").value("COMPLETED"))
                .andExpect(jsonPath("$.agentTrace.ragRetrieval").value("COMPLETED"))
                .andExpect(jsonPath("$.agentTrace.recommendationAgent").value("COMPLETED"));
    }

    @Test
    void blankQueryReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.query").exists());
    }

    @Test
    void maxResultsAboveFiveReturns400() throws Exception {
        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"dark thriller\",\"maxResults\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.maxResults").exists());
    }

    @Test
    void geminiFailureReturns503() throws Exception {
        when(orchestrator.recommend(any()))
                .thenThrow(new LlmUnavailableException("Gemini quota exceeded"));

        mockMvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"dark thriller\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Gemini quota exceeded"));
    }

    @Test
    void healthEndpoint() throws Exception {
        when(geminiAvailability.isConfigured()).thenReturn(true);

        mockMvc.perform(get("/api/recommendations/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.llmModel").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.maxGeminiCallsPerRequest").value(2));
    }
}
