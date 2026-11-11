package com.cinemind.orchestrator.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cinemind.orchestrator.agent.PreferenceAgent;
import com.cinemind.orchestrator.agent.RecommendationAgent;
import com.cinemind.orchestrator.client.RagServiceClient;
import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.RecommendationRequest;
import com.cinemind.orchestrator.dto.RecommendationResponse;
import com.cinemind.orchestrator.dto.RecommendationResult;
import com.cinemind.orchestrator.dto.rag.RagResult;
import com.cinemind.orchestrator.dto.rag.RagSearchRequest;
import com.cinemind.orchestrator.dto.rag.RagSearchResponse;
import com.cinemind.orchestrator.exception.LlmUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationOrchestratorTest {

    @Mock
    private PreferenceAgent preferenceAgent;
    @Mock
    private RagServiceClient ragServiceClient;
    @Mock
    private RecommendationAgent recommendationAgent;

    private RecommendationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new RecommendationOrchestrator(
                preferenceAgent, ragServiceClient, recommendationAgent,
                new PreferenceValidator(), new HardFilterService(),
                new GeminiAvailability("test-key"), 10, 5);
    }

    @Test
    void followsExactExecutionOrderWithOneCallPerStage() {
        MoviePreferences preferences = preferences(2010, 140, List.of());
        List<RagResult> retrieved = List.of(
                result(17L, 2010, 138, List.of("Thriller")),
                result(18L, 2013, 153, List.of("Thriller")));
        when(preferenceAgent.analyze("dark thriller")).thenReturn(preferences);
        when(ragServiceClient.search(any(RagSearchRequest.class))).thenReturn(rag(retrieved));
        when(recommendationAgent.recommend(any(), anyList(), eq(5)))
                .thenReturn(response(item(17L, "Shutter Island", 2010, 95, "Dark tension")));

        RecommendationResult result = orchestrator.recommend(request("dark thriller", 5));

        InOrder order = inOrder(preferenceAgent, ragServiceClient, recommendationAgent);
        order.verify(preferenceAgent).analyze("dark thriller");
        order.verify(ragServiceClient).search(any(RagSearchRequest.class));
        order.verify(recommendationAgent).recommend(any(), anyList(), eq(5));

        verify(preferenceAgent, times(1)).analyze("dark thriller");
        verify(ragServiceClient, times(1)).search(any(RagSearchRequest.class));
        verify(recommendationAgent, times(1)).recommend(any(), anyList(), eq(5));
        assertThat(result.query()).isEqualTo("dark thriller");
    }

    @Test
    void requestsTopTenDocumentsFromRagWithPreferenceFilters() {
        MoviePreferences preferences = preferences(2010, 140, List.of("Comedy"));
        when(preferenceAgent.analyze(any())).thenReturn(preferences);
        when(ragServiceClient.search(any(RagSearchRequest.class)))
                .thenReturn(rag(List.of(result(17L, 2010, 138, List.of("Thriller")))));
        when(recommendationAgent.recommend(any(), anyList(), eq(5)))
                .thenReturn(response());

        orchestrator.recommend(request("dark thriller", 5));

        ArgumentCaptor<RagSearchRequest> captor =
                ArgumentCaptor.forClass(RagSearchRequest.class);
        verify(ragServiceClient).search(captor.capture());
        RagSearchRequest sent = captor.getValue();
        assertThat(sent.getTopK()).isEqualTo(10);
        assertThat(sent.getQuery()).isEqualTo("dark thriller");
        assertThat(sent.getFilters().getYearFrom()).isEqualTo(2010);
        assertThat(sent.getFilters().getMaxRuntime()).isEqualTo(140);
        assertThat(sent.getFilters().getExcludedGenres()).containsExactly("Comedy");
    }

    @Test
    @SuppressWarnings("unchecked")
    void appliesHardFiltersBeforeAgentTwoSeesCandidates() {
        MoviePreferences preferences = preferences(2010, 140, List.of("Horror"));
        List<RagResult> retrieved = List.of(
                result(1L, 2005, 120, List.of("Drama")),
                result(2L, 2015, 160, List.of("Thriller")),
                result(3L, 2012, 110, List.of("Horror")),
                result(4L, 2011, 120, List.of("Thriller")));
        when(preferenceAgent.analyze(any())).thenReturn(preferences);
        when(ragServiceClient.search(any(RagSearchRequest.class))).thenReturn(rag(retrieved));
        when(recommendationAgent.recommend(any(), anyList(), eq(5)))
                .thenReturn(response(item(4L, "Movie 4", 2011, 80, "Good match")));

        RecommendationResult result = orchestrator.recommend(request("q", 5));

        ArgumentCaptor<List<RagResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(recommendationAgent).recommend(any(), captor.capture(), eq(5));
        assertThat(captor.getValue()).extracting(RagResult::getMovieId).containsExactly(4L);
        assertThat(result.agentTrace().retrievedDocuments()).isEqualTo(4);
        assertThat(result.agentTrace().candidatesAfterFilters()).isEqualTo(1);
    }

    @Test
    void dropsInventedMoviesAndUsesRealRetrievedMetadata() {
        MoviePreferences preferences = preferences(null, null, List.of());
        List<RagResult> retrieved = List.of(
                result(17L, 2010, 138, List.of("Thriller")));
        RagResult real = retrieved.get(0);
        real.setTitle("Shutter Island");

        when(preferenceAgent.analyze(any())).thenReturn(preferences);
        when(ragServiceClient.search(any(RagSearchRequest.class))).thenReturn(rag(retrieved));
        when(recommendationAgent.recommend(any(), anyList(), eq(5)))
                .thenReturn(response(
                        item(17L, "COMPLETELY INVENTED TITLE", 9999, 150, "Dark tension"),
                        item(999L, "Ghost Movie", 2020, 99, "Not in context")));

        RecommendationResult result = orchestrator.recommend(request("q", 5));

        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().get(0).movieId()).isEqualTo(17L);
        assertThat(result.recommendations().get(0).title()).isEqualTo("Shutter Island");
        assertThat(result.recommendations().get(0).releaseYear()).isEqualTo(2010);
        assertThat(result.recommendations().get(0).matchScore()).isEqualTo(100);
        assertThat(result.recommendations().get(0).reason()).isEqualTo("Dark tension");
    }

    @Test
    void limitsResultsAndClampsMaxResults() {
        MoviePreferences preferences = preferences(null, null, List.of());
        List<RagResult> retrieved = List.of(
                result(1L, 2010, 120, List.of("Drama")),
                result(2L, 2011, 121, List.of("Drama")),
                result(3L, 2012, 122, List.of("Drama")),
                result(4L, 2013, 123, List.of("Drama")));
        when(preferenceAgent.analyze(any())).thenReturn(preferences);
        when(ragServiceClient.search(any(RagSearchRequest.class))).thenReturn(rag(retrieved));
        when(recommendationAgent.recommend(any(), anyList(), eq(5)))
                .thenReturn(response(
                        item(1L, "M1", 2010, 90, "r1"),
                        item(2L, "M2", 2011, 80, "r2"),
                        item(3L, "M3", 2012, 70, "r3"),
                        item(4L, "M4", 2013, 60, "r4")));

        RecommendationResult result = orchestrator.recommend(request("q", 50));

        assertThat(result.maxResults()).isEqualTo(5);
        assertThat(result.recommendations()).hasSize(4);
    }

    @Test
    void traceContainsRealStatusesAndCounts() {
        MoviePreferences preferences = preferences(null, null, List.of());
        when(preferenceAgent.analyze(any())).thenReturn(preferences);
        when(ragServiceClient.search(any(RagSearchRequest.class)))
                .thenReturn(rag(List.of(result(1L, 2010, 120, List.of("Drama")),
                        result(2L, 2011, 121, List.of("Drama")))));
        when(recommendationAgent.recommend(any(), anyList(), eq(5)))
                .thenReturn(response());

        RecommendationResult result = orchestrator.recommend(request("q", 5));

        assertThat(result.agentTrace().preferenceAgent()).isEqualTo("COMPLETED");
        assertThat(result.agentTrace().ragRetrieval()).isEqualTo("COMPLETED");
        assertThat(result.agentTrace().recommendationAgent()).isEqualTo("COMPLETED");
        assertThat(result.retrievedDocuments()).isEqualTo(2);
        assertThat(result.agentTrace().retrievedDocuments()).isEqualTo(2);
        assertThat(result.agentTrace().candidatesAfterFilters()).isEqualTo(2);
        assertThat(result.preferences()).isNotNull();
    }

    @Test
    void missingCredentialsShortCircuitBeforeAnyCall() {
        RecommendationOrchestrator unconfigured = new RecommendationOrchestrator(
                preferenceAgent, ragServiceClient, recommendationAgent,
                new PreferenceValidator(), new HardFilterService(),
                new GeminiAvailability("not-configured"), 10, 5);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> unconfigured.recommend(request("q", 5)))
                .isInstanceOf(com.cinemind.orchestrator.exception.LlmUnavailableException.class)
                .hasMessageContaining("GEMINI_API_KEY is not configured");

        verify(preferenceAgent, never()).analyze(org.mockito.ArgumentMatchers.anyString());
        verify(ragServiceClient, never()).search(any(RagSearchRequest.class));
    }

    @Test
    void agentOneFailureShortCircuitsAndSavesCredits() {
        when(preferenceAgent.analyze(any()))
                .thenThrow(new LlmUnavailableException("Gemini quota exceeded"));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> orchestrator.recommend(request("q", 5)))
                .isInstanceOf(LlmUnavailableException.class);

        verify(ragServiceClient, never()).search(any(RagSearchRequest.class));
        verify(recommendationAgent, never()).recommend(any(), anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    private static RecommendationRequest request(String query, Integer maxResults) {
        RecommendationRequest request = new RecommendationRequest();
        request.setQuery(query);
        request.setMaxResults(maxResults);
        return request;
    }

    private static MoviePreferences preferences(Integer minYear, Integer maxRuntime,
                                                 List<String> excluded) {
        MoviePreferences preferences = new MoviePreferences();
        preferences.setGenres(List.of("Thriller"));
        preferences.setMood("Dark");
        preferences.setReleaseYearMin(minYear);
        preferences.setMaxRuntime(maxRuntime);
        preferences.setExcludedGenres(excluded);
        return preferences;
    }

    private static RagResult result(Long id, int year, int runtime, List<String> genres) {
        return new RagResult(id, "Movie " + id, genres, year, runtime,
                new BigDecimal("8.0"), 0.6);
    }

    private static RagSearchResponse rag(List<RagResult> results) {
        RagSearchResponse response = new RagSearchResponse();
        response.setResults(new ArrayList<>(results));
        response.setRetrievedCount(results.size());
        return response;
    }

    private static RecommendationResponse response(
            RecommendationResponse.RecommendationItem... items) {
        RecommendationResponse response = new RecommendationResponse();
        response.setRecommendations(List.of(items));
        return response;
    }

    private static RecommendationResponse.RecommendationItem item(
            Long movieId, String title, Integer year, Integer score, String reason) {
        RecommendationResponse.RecommendationItem item =
                new RecommendationResponse.RecommendationItem();
        item.setMovieId(movieId);
        item.setTitle(title);
        item.setReleaseYear(year);
        item.setMatchScore(score);
        item.setReason(reason);
        return item;
    }
}
