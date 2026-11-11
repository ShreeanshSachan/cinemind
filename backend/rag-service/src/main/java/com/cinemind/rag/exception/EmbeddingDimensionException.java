package com.cinemind.rag.exception;

/** The model returned a vector with an unexpected dimension (configuration problem). Maps to 500. */
public class EmbeddingDimensionException extends RuntimeException {

    public EmbeddingDimensionException(int actual, int expected) {
        super("Embedding dimension mismatch: model returned " + actual
                + " values but EMBEDDING_DIMENSION is " + expected);
    }
}
