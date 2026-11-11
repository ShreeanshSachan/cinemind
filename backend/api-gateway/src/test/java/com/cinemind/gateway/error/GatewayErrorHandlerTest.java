package com.cinemind.gateway.error;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayErrorHandlerTest {

    @Test
    void connectionRefusedBecomesServiceUnavailable() {
        assertThat(GatewayErrorHandler.resolveStatus(new ConnectException("Connection refused")))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void missingInstanceMessageBecomesServiceUnavailable() {
        assertThat(GatewayErrorHandler.resolveStatus(
                new IllegalStateException("Unable to find instance for MOVIE-SERVICE")))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void wrappedCauseIsInspected() {
        Throwable wrapped = new RuntimeException("gateway failure",
                new ConnectException("Connection refused"));

        assertThat(GatewayErrorHandler.resolveStatus(wrapped))
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void notFoundIsPreserved() {
        assertThat(GatewayErrorHandler.resolveStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unknownFailureBecomesInternalServerError() {
        assertThat(GatewayErrorHandler.resolveStatus(new RuntimeException("boom")))
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void writesStructuredJsonServiceUnavailableResponse() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GatewayErrorHandler handler = new GatewayErrorHandler(mapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/movies"));

        handler.handle(exchange, new ConnectException("Connection refused")).block();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getHeaders().getContentType())
                .isEqualTo(MediaType.APPLICATION_JSON);

        JsonNode body = mapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(body.get("status").asInt()).isEqualTo(503);
        assertThat(body.get("error").asText()).isEqualTo("Service Unavailable");
        assertThat(body.get("path").asText()).isEqualTo("/api/movies");
        assertThat(body.get("message").asText()).contains("Downstream service is unavailable");
        assertThat(body.has("timestamp")).isTrue();
    }
}
