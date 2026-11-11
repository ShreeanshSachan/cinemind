package com.cinemind.movie.exception;

/** Raised when a movie id does not exist. Maps to HTTP 404. */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long id) {
        super("Movie with id " + id + " was not found");
    }
}
