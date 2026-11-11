package com.cinemind.rag.util;

/** Pure Java cosine similarity. No vector database, no external library. */
public final class CosineSimilarity {

    private CosineSimilarity() {
    }

    /**
     * Returns dot(a,b) / (|a| * |b|), in the range [-1, 1].
     * A zero-length vector yields 0.0 so ranking stays deterministic.
     */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Cosine similarity needs two non-null vectors");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Vector dimensions differ: " + a.length + " vs " + b.length);
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * (double) b[i];
            normA += (double) a[i] * (double) a[i];
            normB += (double) b[i] * (double) b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        double similarity = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        // Guard against tiny floating point overshoot outside [-1, 1].
        return Math.max(-1.0, Math.min(1.0, similarity));
    }
}
