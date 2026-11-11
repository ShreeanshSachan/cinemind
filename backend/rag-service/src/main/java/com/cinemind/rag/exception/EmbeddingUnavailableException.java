package com.cinemind.rag.exception;

/** Ollama is unreachable or the embedding model could not produce a vector. Maps to 503. */
public class EmbeddingUnavailableException extends RuntimeException {

    public EmbeddingUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmbeddingUnavailableException(String message) {
        super(message);
    }
}
