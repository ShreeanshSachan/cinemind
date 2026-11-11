package com.cinemind.orchestrator.prompt;

/** Concise, single-shot prompt template for Agent 1. */
public final class PreferencePrompt {

    private PreferencePrompt() {
    }

    public static String build(String query, String outputFormat) {
        return """
                You are a movie preference analyst for a recommendation system.
                Extract structured preferences from the user's request.

                Rules:
                - Output ONLY a RFC8259 compliant JSON object. No prose, no markdown fences.
                - genres: preferred genre names; empty array if none are implied.
                - mood: a short mood or tone phrase; empty string if none.
                - similarTo: reference movie titles the user mentions; empty array if none.
                - releaseYearMin: integer minimum release year, or null if none.
                - maxRuntime: integer maximum runtime in minutes, or null if none.
                - excludedGenres: genres the user explicitly wants to avoid; empty array if none.
                - Never invent values the user did not imply.

                User request: %s

                %s
                """.formatted(query, outputFormat);
    }
}
