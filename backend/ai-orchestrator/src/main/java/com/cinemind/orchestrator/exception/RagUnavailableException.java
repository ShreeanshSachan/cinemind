package com.cinemind.orchestrator.exception;

/** RAG Service could not be resolved via Eureka or did not answer. Maps to 503. */
public class RagUnavailableException extends RuntimeException {

    public RagUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public RagUnavailableException(String message) {
        super(message);
    }
}
