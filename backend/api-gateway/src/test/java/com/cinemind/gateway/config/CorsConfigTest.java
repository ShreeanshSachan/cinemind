package com.cinemind.gateway.config;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    /**
     * The reactive CorsUtils compares the Origin against the request URI, so the
     * mock request must use an absolute URL (scheme + host), otherwise the
     * request is rejected with "Actual request scheme must not be null".
     */
    private static MockServerWebExchange preflight(String origin) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost:8080/api/movies")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "GET"));
    }

    @Test
    void allowsAngularDevServerAndStandardMethods() {
        CorsConfiguration config = CorsConfig.corsConfiguration("http://localhost:4200");

        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(config.getAllowedHeaders()).contains("*");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void supportsCommaSeparatedOriginsAndTrimsWhitespace() {
        CorsConfiguration config = CorsConfig.corsConfiguration(
                "http://localhost:4200, https://cinemind.example.com");

        assertThat(config.getAllowedOrigins()).containsExactly(
                "http://localhost:4200", "https://cinemind.example.com");
    }

    @Test
    void filterShortCircuitsAngularPreflightWithAllowHeaders() {
        CorsWebFilter filter = new CorsConfig().corsWebFilter("http://localhost:4200");
        MockServerWebExchange exchange = preflight("http://localhost:4200");
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        WebFilterChain chain = e -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        // A successful preflight is short-circuited by the filter. The reactive
        // DefaultCorsProcessor sets headers but leaves the status untouched
        // (the real server then defaults to 200 OK), so assert the headers.
        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowOrigin())
                .isEqualTo("http://localhost:4200");
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowCredentials())
                .isTrue();
        assertThat(exchange.getResponse().getHeaders().getAccessControlAllowMethods())
                .contains(org.springframework.http.HttpMethod.GET);
    }

    @Test
    void filterRejectsUnknownOrigin() {
        CorsWebFilter filter = new CorsConfig().corsWebFilter("http://localhost:4200");
        MockServerWebExchange exchange = preflight("https://evil.example.com");
        AtomicBoolean chainInvoked = new AtomicBoolean(false);
        WebFilterChain chain = e -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(chainInvoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
