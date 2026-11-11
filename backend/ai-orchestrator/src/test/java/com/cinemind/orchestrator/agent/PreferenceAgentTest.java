package com.cinemind.orchestrator.agent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.exception.LlmUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreferenceAgentTest {

    @Mock
    private ChatModel chatModel;

    private PreferenceAgent agent;

    @BeforeEach
    void setUp() {
        agent = new PreferenceAgent(chatModel);
    }

    @Test
    void parsesStructuredPreferencesWithExactlyOneGeminiCall() {
        when(chatModel.call(anyString())).thenReturn("""
                {"genres":["Psychological Thriller"],"mood":"Mind-bending",
                 "similarTo":["Shutter Island"],"releaseYearMin":2010,
                 "maxRuntime":140,"excludedGenres":["Comedy"]}
                """);

        MoviePreferences preferences = agent.analyze(
                "I want a mind-bending psychological thriller after 2010 under 140 minutes");

        assertThat(preferences.getGenres()).containsExactly("Psychological Thriller");
        assertThat(preferences.getMood()).isEqualTo("Mind-bending");
        assertThat(preferences.getSimilarTo()).containsExactly("Shutter Island");
        assertThat(preferences.getReleaseYearMin()).isEqualTo(2010);
        assertThat(preferences.getMaxRuntime()).isEqualTo(140);
        assertThat(preferences.getExcludedGenres()).containsExactly("Comedy");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(chatModel, times(1)).call(prompt.capture());
        assertThat(prompt.getValue()).contains("I want a mind-bending psychological thriller");
        assertThat(prompt.getValue()).contains("releaseYearMin");
        assertThat(prompt.getValue()).contains("JSON");
    }

    @Test
    void handlesMarkdownFencedJson() {
        when(chatModel.call(anyString())).thenReturn(
                "```json\n{\"genres\":[\"Drama\"],\"mood\":\"\",\"similarTo\":[],"
                        + "\"releaseYearMin\":null,\"maxRuntime\":null,\"excludedGenres\":[]}\n```");

        MoviePreferences preferences = agent.analyze("something dramatic");

        assertThat(preferences.getGenres()).containsExactly("Drama");
        verify(chatModel, times(1)).call(anyString());
    }

    @Test
    void reportsUnparseableOutput() {
        when(chatModel.call(anyString())).thenReturn("Sorry, I cannot do that.");

        assertThatThrownBy(() -> agent.analyze("query"))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("could not be parsed");
        verify(chatModel, times(1)).call(anyString());
    }

    @Test
    void wrapsGeminiFailureWithoutRetrying() {
        when(chatModel.call(anyString())).thenThrow(new RuntimeException("429 quota exceeded"));

        assertThatThrownBy(() -> agent.analyze("query"))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("429 quota exceeded");
        // No retries: exactly one call.
        verify(chatModel, times(1)).call(anyString());
    }

    @Test
    void reportsEmptyResponse() {
        when(chatModel.call(anyString())).thenReturn("   ");

        assertThatThrownBy(() -> agent.analyze("query"))
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("empty");
    }
}
