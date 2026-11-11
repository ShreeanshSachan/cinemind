package com.cinemind.rag.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CosineSimilarityTest {

    @Test
    void identicalVectorsScoreOne() {
        float[] a = {1f, 2f, 3f, 4f};
        assertThat(CosineSimilarity.cosine(a, a)).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void orthogonalVectorsScoreZero() {
        assertThat(CosineSimilarity.cosine(
                new float[]{1f, 0f, 0f}, new float[]{0f, 1f, 0f}))
                .isCloseTo(0.0, within(1e-9));
    }

    @Test
    void oppositeVectorsScoreMinusOne() {
        assertThat(CosineSimilarity.cosine(
                new float[]{1f, 2f}, new float[]{-1f, -2f}))
                .isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void knownNumericExample() {
        // dot=32, |a|=sqrt(14), |b|=sqrt(77) -> 32/sqrt(1078)
        double expected = 32.0 / Math.sqrt(14.0 * 77.0);
        assertThat(CosineSimilarity.cosine(
                new float[]{1f, 2f, 3f}, new float[]{4f, 5f, 6f}))
                .isCloseTo(expected, within(1e-9));
    }

    @Test
    void scalingAVectorDoesNotChangeSimilarity() {
        float[] a = {1f, 2f, 3f};
        float[] scaled = {10f, 20f, 30f};
        float[] b = {3f, 2f, 1f};
        assertThat(CosineSimilarity.cosine(a, b))
                .isCloseTo(CosineSimilarity.cosine(scaled, b), within(1e-9));
    }

    @Test
    void symmetric() {
        float[] a = {1f, 5f, -2f};
        float[] b = {3f, 0f, 4f};
        assertThat(CosineSimilarity.cosine(a, b))
                .isCloseTo(CosineSimilarity.cosine(b, a), within(1e-12));
    }

    @Test
    void zeroVectorScoresZeroInsteadOfNaN() {
        assertThat(CosineSimilarity.cosine(new float[]{0f, 0f}, new float[]{1f, 1f}))
                .isEqualTo(0.0);
        assertThat(CosineSimilarity.cosine(new float[]{1f, 1f}, new float[]{0f, 0f}))
                .isEqualTo(0.0);
    }

    @Test
    void staysWithinMinusOneAndOne() {
        assertThat(CosineSimilarity.cosine(
                new float[]{3f, 4f}, new float[]{-3f, -4f}))
                .isBetween(-1.0, 1.0);
    }

    @Test
    void mismatchedDimensionsAreRejected() {
        assertThatThrownBy(() -> CosineSimilarity.cosine(
                new float[]{1f, 2f}, new float[]{1f}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimensions differ");
    }

    @Test
    void nullVectorsAreRejected() {
        assertThatThrownBy(() -> CosineSimilarity.cosine(null, new float[]{1f}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CosineSimilarity.cosine(new float[]{1f}, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
