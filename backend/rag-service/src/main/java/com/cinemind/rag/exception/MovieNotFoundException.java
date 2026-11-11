package com.cinemind.rag.exception;

/** The requested movie id does not exist in the Movie Service. Maps to 404. */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long movieId) {
        super("Movie with id " + movieId + " was not found in the Movie Service");
    }
}
