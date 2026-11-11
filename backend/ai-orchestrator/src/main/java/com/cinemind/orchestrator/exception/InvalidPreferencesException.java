package com.cinemind.orchestrator.exception;

/** Agent 1 produced preferences that cannot be validated. Maps to 502. */
public class InvalidPreferencesException extends RuntimeException {

    public InvalidPreferencesException(String message) {
        super(message);
    }
}
