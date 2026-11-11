package com.cinemind.orchestrator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cinemind.orchestrator.exception.LlmUnavailableException;

/**
 * Guards Gemini credentials. When GEMINI_API_KEY is absent the service still
 * starts (Spring AI needs a non-blank key to build its client), reports
 * DEGRADED health, and refuses recommendations BEFORE any outbound call.
 */
@Component
public class GeminiAvailability {

    static final String NOT_CONFIGURED = "not-configured";

    private final boolean configured;

    public GeminiAvailability(
            @Value("${spring.ai.google.genai.api-key:not-configured}") String apiKey) {
        String key = apiKey == null ? "" : apiKey.trim();
        this.configured = !key.isEmpty()
                && !NOT_CONFIGURED.equals(key)
                && !"your-gemini-api-key-here".equals(key);
    }

    public boolean isConfigured() {
        return configured;
    }

    public void requireConfigured() {
        if (!configured) {
            throw new LlmUnavailableException(
                    "GEMINI_API_KEY is not configured. Set it in .env before requesting recommendations.");
        }
    }
}
