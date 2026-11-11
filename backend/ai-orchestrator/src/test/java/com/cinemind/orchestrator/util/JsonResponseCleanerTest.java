package com.cinemind.orchestrator.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonResponseCleanerTest {

    @Test
    void keepsPlainJson() {
        assertThat(JsonResponseCleaner.clean("  {\"a\":1}  ")).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsMarkdownFences() {
        assertThat(JsonResponseCleaner.clean("```json\n{\"a\":1}\n```"))
                .isEqualTo("{\"a\":1}");
    }

    @Test
    void stripsSurroundingProse() {
        assertThat(JsonResponseCleaner.clean("Here is the result: {\"a\":1} hope that helps"))
                .isEqualTo("{\"a\":1}");
    }

    @Test
    void handlesNull() {
        assertThat(JsonResponseCleaner.clean(null)).isEmpty();
    }
}
