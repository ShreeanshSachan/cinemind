package com.cinemind.movie.exception;

/** Raised when another movie already uses the same title and release year. Maps to HTTP 409. */
public class DuplicateMovieException extends RuntimeException {

    public DuplicateMovieException(String title, Integer releaseYear) {
        super("A movie titled '" + title + "' released in " + releaseYear + " already exists");
    }
}
