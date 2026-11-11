package com.cinemind.orchestrator.agent;

import java.util.List;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.cinemind.orchestrator.dto.MoviePreferences;
import com.cinemind.orchestrator.dto.RecommendationResponse;
import com.cinemind.orchestrator.dto.rag.RagResult;
import com.cinemind.orchestrator.exception.LlmUnavailableException;
import com.cinemind.orchestrator.prompt.RecommendationPrompt;
import com.cinemind.orchestrator.util.JsonResponseCleaner;

/**
 * AGENT 2 - Recommendation Specialist.
 * Exactly ONE Gemini Flash call with a compact candidate list. It never calls
 * Agent 1 or the RAG Service itself. The orchestrator validates its output.
 */
@Component
public class RecommendationAgent {

    private final ChatModel chatModel;
    private final BeanOutputConverter<RecommendationResponse> converter =
            new BeanOutputConverter<>(RecommendationResponse.class);

    public RecommendationAgent(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public RecommendationResponse recommend(MoviePreferences preferences,
                                             List<RagResult> candidates,
                                             int maxResults) {
        String prompt = RecommendationPrompt.build(
                preferences, candidates, maxResults, converter.getFormat());

        String raw;
        try {
            raw = chatModel.call(prompt);
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException(
                    "Gemini recommendation ranking failed: " + ex.getMessage(), ex);
        }
        if (raw == null || raw.isBlank()) {
            throw new LlmUnavailableException("Gemini returned an empty recommendation response");
        }

        try {
            RecommendationResponse parsed = converter.convert(JsonResponseCleaner.clean(raw));
            if (parsed == null) {
                throw new IllegalStateException("Parsed recommendations were null");
            }
            return parsed;
        } catch (RuntimeException ex) {
            throw new LlmUnavailableException(
                    "Gemini returned recommendations that could not be parsed as JSON", ex);
        }
    }
}
