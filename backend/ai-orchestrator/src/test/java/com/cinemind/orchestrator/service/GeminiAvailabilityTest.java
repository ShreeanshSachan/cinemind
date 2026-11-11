package com.cinemind.orchestrator.service;

import org.junit.jupiter.api.Test;

import com.cinemind.orchestrator.exception.LlmUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiAvailabilityTest {

    @Test
    void realKeyIsConfigured() {
        GeminiAvailability availability = new GeminiAvailability("AIza-real-looking-key");

        assertThat(availability.isConfigured()).isTrue();
        availability.requireConfigured(); // does not throw
    }

    @Test
    void blankKeyIsNotConfigured() {
        GeminiAvailability availability = new GeminiAvailability("  ");

        assertThat(availability.isConfigured()).isFalse();
        assertThatThrownBy(availability::requireConfigured)
                .isInstanceOf(LlmUnavailableException.class)
                .hasMessageContaining("GEMINI_API_KEY is not configured");
    }

    @Test
    void sentinelAndEnvPlaceholderAreNotConfigured() {
        assertThat(new GeminiAvailability("not-configured").isConfigured()).isFalse();
        assertThat(new GeminiAvailability("your-gemini-api-key-here").isConfigured()).isFalse();
        assertThat(new GeminiAvailability(null).isConfigured()).isFalse();
    }
}
