package com.cinemind.orchestrator;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.cinemind.orchestrator.client.RagServiceClient;
import com.cinemind.orchestrator.dto.rag.RagResult;
import com.cinemind.orchestrator.dto.rag.RagSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Full HTTP -> controller -> orchestrator -> real agents (prompt + parsing) ->
 * real validator/filters. Only ChatModel and RagServiceClient are mocked, so
 * the whole pipeline is exercised without a single Gemini credit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrchestratorIntegrationTest {

    private static final String PREFERENCES_JSON = """
            {"genres":["Psychological Thriller"],"mood":"Mind-bending",
             "similarTo":["Shutter Island"],"releaseYearMin":2010,
             "maxRuntime":140,"excludedGenres":[]}
            """;

    private static final String RECOMMENDATIONS_JSON = """
            {"recommendations":[
              {"movieId":17,"title":"INVENTED TITLE","releaseYear":9999,
               "matchScore":95,"reason":"Dark psychological tension"},
              {"movieId":999,"title":"Ghost","releaseYear":2020,
               "matchScore":99,"reason":"Not a real candidate"}
            ]}
            """;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private RagServiceClient ragServiceClient;

    @Autowired
    private Environment environment;

    @Test
    void creditGuardsAreActive() {
        // Spring AI defaults to 10 retries; we must never retry a Gemini call.
        assertThat(environment.getProperty("spring.ai.retry.max-attempts")).isEqualTo("1");
        assertThat(environment.getProperty("spring.ai.retry.on-client-errors")).isEqualTo("false");
        assertThat(environment.getProperty("spring.ai.google.genai.chat.options.max-output-tokens"))
                .isEqualTo("1024");
        assertThat(environment.getProperty("cinemind.orchestrator.max-recommendations"))
                .isEqualTo("5");
        assertThat(environment.getProperty("cinemind.orchestrator.rag-top-k"))
                .isEqualTo("10");
    }

    @Test
    void fullPipelineReturnsOnlyRealMoviesWithExactlyTwoGeminiCalls() throws Exception {
        when(chatModel.call(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            return prompt.contains("preference analyst")
                    ? PREFERENCES_JSON
                    : RECOMMENDATIONS_JSON;
        });

        RagSearchResponse rag = new RagSearchResponse();
        rag.setResults(List.of(
                new RagResult(17L, "Shutter Island", List.of("Mystery", "Thriller"),
                        2010, 138, new BigDecimal("8.2"), 0.6929),
                new RagResult(24L, "Black Swan", List.of("Drama", "Thriller"),
                        2010, 108, new BigDecimal("8.0"), 0.5553)));
        rag.setRetrievedCount(2);
        when(ragServiceClient.search(any())).thenReturn(rag);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/recommendations",
                new HttpEntity<>(
                        "{\"query\":\"mind bending psychological thriller after 2010\",\"maxResults\":5}",
                        headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("query").asText())
                .isEqualTo("mind bending psychological thriller after 2010");
        assertThat(body.get("retrievedDocuments").asInt()).isEqualTo(2);
        assertThat(body.get("preferences").get("genres").get(0).asText())
                .isEqualTo("Psychological Thriller");
        assertThat(body.get("preferences").get("releaseYearMin").asInt()).isEqualTo(2010);
        assertThat(body.get("agentTrace").get("preferenceAgent").asText()).isEqualTo("COMPLETED");
        assertThat(body.get("agentTrace").get("ragRetrieval").asText()).isEqualTo("COMPLETED");
        assertThat(body.get("agentTrace").get("recommendationAgent").asText()).isEqualTo("COMPLETED");

        // The invented movie id and invented metadata are discarded.
        assertThat(body.get("recommendations").size()).isEqualTo(1);
        JsonNode first = body.get("recommendations").get(0);
        assertThat(first.get("movieId").asLong()).isEqualTo(17L);
        assertThat(first.get("title").asText()).isEqualTo("Shutter Island");
        assertThat(first.get("releaseYear").asInt()).isEqualTo(2010);
        assertThat(first.get("matchScore").asInt()).isEqualTo(95);
        assertThat(first.get("reason").asText()).isEqualTo("Dark psychological tension");

        // Exactly one Gemini call per agent; no retries or loops.
        verify(chatModel, times(2)).call(anyString());
        verify(ragServiceClient, times(1)).search(any());
    }

    @Test
    void healthEndpointWorksWithoutAnyGeminiCall() throws Exception {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/api/recommendations/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("llmModel").asText()).isEqualTo("gemini-2.5-flash");
        assertThat(body.get("maxGeminiCallsPerRequest").asInt()).isEqualTo(2);
        verify(chatModel, times(0)).call(anyString());
    }
}
