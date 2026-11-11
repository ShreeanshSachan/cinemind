package com.cinemind.rag.service;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class OllamaStatusServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void detectsAPulledTaggedModel() throws Exception {
        JsonNode root = MAPPER.readTree(
                "{\"models\":[{\"name\":\"nomic-embed-text:latest\"}]}");
        assertThat(OllamaStatusService.isModelPresent(root, "nomic-embed-text")).isTrue();
    }

    @Test
    void detectsAPulledModelWithoutTag() throws Exception {
        JsonNode root = MAPPER.readTree(
                "{\"models\":[{\"name\":\"nomic-embed-text\"}]}");
        assertThat(OllamaStatusService.isModelPresent(root, "nomic-embed-text")).isTrue();
    }

    @Test
    void reportsMissingModel() throws Exception {
        JsonNode root = MAPPER.readTree(
                "{\"models\":[{\"name\":\"llama3:latest\"}]}");
        assertThat(OllamaStatusService.isModelPresent(root, "nomic-embed-text")).isFalse();
    }

    @Test
    void handlesMalformedTagsResponse() throws Exception {
        JsonNode root = MAPPER.readTree("{\"models\":null}");
        assertThat(OllamaStatusService.isModelPresent(root, "nomic-embed-text")).isFalse();
        assertThat(OllamaStatusService.isModelPresent(null, "nomic-embed-text")).isFalse();
    }

    @Test
    void unreachableOllamaIsReportedGracefully() {
        // Port 1 is not listening: the probe must return, not throw.
        OllamaStatusService service =
                new OllamaStatusService("http://127.0.0.1:1", "nomic-embed-text");

        OllamaStatusService.OllamaStatus status = service.check();

        assertThat(status.available()).isFalse();
        assertThat(status.modelAvailable()).isFalse();
        assertThat(status.message()).contains("unreachable");
    }
}
