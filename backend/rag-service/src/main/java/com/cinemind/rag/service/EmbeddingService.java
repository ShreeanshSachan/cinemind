package com.cinemind.rag.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cinemind.rag.exception.EmbeddingDimensionException;
import com.cinemind.rag.exception.EmbeddingUnavailableException;

/**
 * Thin wrapper over Spring AI's Ollama EmbeddingModel. Returns numerical
 * vectors and validates their dimension. Any Ollama connection/model failure
 * becomes a controlled EmbeddingUnavailableException (HTTP 503), never a
 * raw stack trace.
 */
@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingModel embeddingModel;
    private final int expectedDimension;
    private final String modelName;

    public EmbeddingService(
            EmbeddingModel embeddingModel,
            @Value("${cinemind.rag.embedding-dimension:768}") int expectedDimension,
            @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}") String modelName) {
        this.embeddingModel = embeddingModel;
        this.expectedDimension = expectedDimension;
        this.modelName = modelName;
    }

    /** One embedding call. Used once per query and once per new/updated movie. */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Cannot embed a blank document");
        }

        float[] vector;
        try {
            vector = embeddingModel.embed(text);
        } catch (RuntimeException ex) {
            log.warn("Ollama embedding call failed for model {}", modelName, ex);
            throw new EmbeddingUnavailableException(
                    "Ollama embedding failed for model '" + modelName + "': " + rootCauseMessage(ex)
                            + ". Check OLLAMA_BASE_URL and that the model is pulled (ollama pull "
                            + modelName + ").", ex);
        }
        validate(vector);
        return vector;
    }

    private static String rootCauseMessage(Throwable ex) {
        Throwable current = ex;
        for (int depth = 0; current.getCause() != null && depth < 8; depth++) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank()
                ? current.getClass().getSimpleName()
                : message;
    }

    private void validate(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new EmbeddingUnavailableException("Ollama returned an empty embedding");
        }
        if (vector.length != expectedDimension) {
            throw new EmbeddingDimensionException(vector.length, expectedDimension);
        }
        double squaredSum = 0.0;
        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new EmbeddingUnavailableException("Ollama returned a non-finite embedding value");
            }
            squaredSum += (double) value * value;
        }
        if (squaredSum == 0.0) {
            throw new EmbeddingUnavailableException("Ollama returned an all-zero embedding");
        }
    }

    public int dimensions() {
        return expectedDimension;
    }

    public String modelName() {
        return modelName;
    }
}
