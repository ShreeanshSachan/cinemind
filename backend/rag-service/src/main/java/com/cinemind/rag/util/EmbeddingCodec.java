package com.cinemind.rag.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Stores a float[] as a portable JSON array in a MySQL TEXT column. */
public final class EmbeddingCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EmbeddingCodec() {
    }

    public static String encode(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("Cannot encode a null or empty embedding");
        }
        try {
            return MAPPER.writeValueAsString(vector);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize embedding", ex);
        }
    }

    public static float[] decode(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Cannot decode a blank embedding");
        }
        try {
            return MAPPER.readValue(json, float[].class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored embedding is not a valid JSON float array", ex);
        }
    }
}
