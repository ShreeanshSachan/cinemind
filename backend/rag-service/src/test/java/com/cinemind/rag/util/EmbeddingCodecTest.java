package com.cinemind.rag.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingCodecTest {

    @Test
    void roundTripsFloatArray() {
        float[] vector = {0.1f, -0.25f, 0.75f, 1.0f};
        String json = EmbeddingCodec.encode(vector);

        assertThat(json).startsWith("[").endsWith("]");
        assertThat(EmbeddingCodec.decode(json)).containsExactly(vector);
    }

    @Test
    void encodesAsJsonFloatArray() {
        assertThat(EmbeddingCodec.encode(new float[]{1.0f, 2.0f}))
                .isEqualTo("[1.0,2.0]");
    }

    @Test
    void rejectsEmptyVector() {
        assertThatThrownBy(() -> EmbeddingCodec.encode(new float[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankStoredValue() {
        assertThatThrownBy(() -> EmbeddingCodec.decode("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedStoredValue() {
        assertThatThrownBy(() -> EmbeddingCodec.decode("not-json"))
                .isInstanceOf(IllegalStateException.class);
    }
}
