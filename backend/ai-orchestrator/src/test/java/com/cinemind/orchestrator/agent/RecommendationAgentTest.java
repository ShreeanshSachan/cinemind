package com.cinemind.orchestrator.agent;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.RecommendationResponse;
import com.cinemind.orchestrator.dto.rag.RagResult;
import com.cinemind.orchestrator.exception.LlmUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationAgentTest {

    @Mock
    private ChatModel chatModel;

    private RecommendationAgent agent;

    @BeforeEach
    void setUp() {
        agent = new RecommendationAgent(chatModel);
    }

    @Test
    void parsesRankingWithExactlyOneGeminiCall() {
        when(chatModel.call(anyString())).thenReturn("""
                {"recommendations":[
                  {"movieId":17,"title":"Shutter Island","releaseYear":2010,
                   "matchScore":95,"reason":"Dark psychological tension"},
                  {"movieId":18,"title":"Prisoners","releaseYear":2013,
                   "matchScore":88,"reason":"Bleak investigation mood"}
                ]}
                """);

        MoviePreferences preferences = new MoviePreferences();
        preferences.getGenres().add("Thriller");

        RecommendationResponse response = agent.recommend(preferences, candidates(), 5);

        assertThat(response.getRecommendations()).hasSize(2);
        assertThat(response.getRecommendations().get(0).getMovieId()).isEqualTo(17L);
        assertThat(response.getRecommendations().get(0).getMatchScore()).isEqualTo(95);
        assertThat(response.getRecommendations().get(0).getReason())
                .isEqualTo("Dark psychological tension");
        verify(chatModel, times(1)).call(anyString());
    }

    @Test
    void promptUsesCompactContextOnly() {
        when(chatModel.call(anyString())).thenReturn("{\"recommendations\":[]}");

        agent.recommend(new MoviePreferences(), candidates(), 5);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatModel).call(prompt.capture());
        String text = prompt.getValue();
        assertThat(text).contains("Shutter Island");
        assertThat(text).contains("0.6929");
        // No bulky fields leak into the Agent 2 context.
        assertThat(text).doesNotContain("A U.S. Marshal investigates");
        assertThat(text).doesNotContain("documentContent");
        assertThat(text).doesNotContain("posterUrl");
        assertThat(text).contains("Return at most 5 recommendations");
        assertThat(text).contains("Never invent movie IDs");
    }

    @Test
    void reportsInvalidJsonWithoutRetry() {
        when(chatModel.call(anyString())).thenReturn("not json at all");

        assertThatThrownBy(() -> agent.recommend(new MoviePreferences(), candidates(), 5))
                .isInstanceOf(LlmUnavailableException.class);
        verify(chatModel, times(1)).call(anyString());
    }

    @Test
    void wrapsGeminiFailure() {
        when(chatModel.call(anyString())).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> agent.recommend(new MoviePreferences(), candidates(), 5))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("connection refused");
        verify(chatModel, times(1)).call(anyString());
    }

    private static List<RagResult> candidates() {
        RagResult first = new RagResult(17L, "Shutter Island",
                List.of("Mystery", "Thriller"), 2010, 138,
                new BigDecimal("8.2"), 0.6929);
        first.setDocumentContent("A U.S. Marshal investigates a mysterious island asylum.");
        RagResult second = new RagResult(18L, "Prisoners",
                List.of("Thriller", "Crime"), 2013, 153,
                new BigDecimal("8.1"), 0.544);
        return List.of(first, second);
    }
}
