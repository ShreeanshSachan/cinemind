package com.cinemind.rag.exception;

/** Movie Service could not be resolved via Eureka or did not answer. Maps to 503. */
public class MovieServiceUnavailableException extends RuntimeException {

    public MovieServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public MovieServiceUnavailableException(String message) {
        super(message);
    }
}
