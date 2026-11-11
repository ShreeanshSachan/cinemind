package com.cinemind.rag.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

import com.cinemind.rag.exception.EmbeddingDimensionException;
import com.cinemind.rag.exception.EmbeddingUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingModel, 768, "nomic-embed-text");
    }

    @Test
    void returnsValidatedVector() {
        float[] vector = vector(768, 0.5f);
        when(embeddingModel.embed("a movie")).thenReturn(vector);

        assertThat(embeddingService.embed("a movie")).isSameAs(vector);
        assertThat(embeddingService.dimensions()).isEqualTo(768);
        assertThat(embeddingService.modelName()).isEqualTo("nomic-embed-text");
    }

    @Test
    void rejectsWrongDimension() {
        when(embeddingModel.embed(anyString())).thenReturn(vector(384, 0.5f));

        assertThatThrownBy(() -> embeddingService.embed("a movie"))
                .isInstanceOf(EmbeddingDimensionException.class)
                .hasMessageContaining("384")
                .hasMessageContaining("768");
    }

    @Test
    void wrapsOllamaConnectionFailure() {
        when(embeddingModel.embed(anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> embeddingService.embed("a movie"))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("Ollama embedding failed")
                .hasMessageContaining("nomic-embed-text");
    }

    @Test
    void rejectsNullVector() {
        when(embeddingModel.embed(anyString())).thenReturn(null);

        assertThatThrownBy(() -> embeddingService.embed("a movie"))
                .isInstanceOf(EmbeddingUnavailableException.class);
    }

    @Test
    void rejectsNonFiniteVector() {
        float[] vector = vector(768, 0.5f);
        vector[10] = Float.NaN;
        when(embeddingModel.embed(anyString())).thenReturn(vector);

        assertThatThrownBy(() -> embeddingService.embed("a movie"))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("non-finite");
    }

    @Test
    void rejectsAllZeroVector() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[768]);

        assertThatThrownBy(() -> embeddingService.embed("a movie"))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("all-zero");
    }

    @Test
    void rejectsBlankTextBeforeCallingModel() {
        assertThatThrownBy(() -> embeddingService.embed("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static float[] vector(int size, float value) {
        float[] vector = new float[size];
        java.util.Arrays.fill(vector, value);
        return vector;
    }
}
