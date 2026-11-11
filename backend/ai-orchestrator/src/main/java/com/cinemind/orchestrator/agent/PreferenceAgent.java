package com.cinemind.orchestrator.agent;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.exception.LlmUnavailableException;
import com.cinemind.orchestrator.prompt.PreferencePrompt;
import com.cinemind.orchestrator.util.JsonResponseCleaner;

/**
 * AGENT 1 - Preference Analyst.
 * Exactly ONE Gemini Flash call, returning structured MoviePreferences JSON.
 * No loops, no retries, no calls to the other agent.
 */
@Component
public class PreferenceAgent {

    private final ChatModel chatModel;
    private final BeanOutputConverter<MoviePreferences> converter =
            new BeanOutputConverter<>(MoviePreferences.class);

    public PreferenceAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public MoviePreferences analyze(String query) {
        String prompt = PreferencePrompt.build(query, converter.getFormat());

        String raw;
        try {
            raw = chatModel.call(prompt);
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException(
                    "Gemini preference extraction failed: " + ex.getMessage(), ex);
        }
        if (raw == null || raw.isBlank()) {
            throw new LlmUnavailableException("Gemini returned an empty preference response");
        }

        try {
            MoviePreferences parsed = converter.convert(JsonResponseCleaner.clean(raw));
            if (parsed == null) {
                throw new IllegalStateException("Parsed preferences were null");
            }
            return parsed;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException(
                    "Gemini returned preferences that could not be parsed as JSON", ex);
        }
    }
}
