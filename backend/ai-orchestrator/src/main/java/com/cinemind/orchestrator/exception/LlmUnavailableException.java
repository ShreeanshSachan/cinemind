package com.cinemind.orchestrator.exception;

/** Gemini call failed or returned unusable structured output. Maps to 503. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmUnavailableException(String message) {
        super(message);
    }
}
