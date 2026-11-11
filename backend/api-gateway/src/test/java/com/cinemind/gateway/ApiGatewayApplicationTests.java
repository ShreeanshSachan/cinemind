package com.cinemind.gateway;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.cors.reactive.CorsWebFilter;

import com.cinemind.gateway.error.GatewayErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false"
        })
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    @Qualifier("cinemindRoutes")
    private RouteLocator cinemindRoutes;

    @Autowired
    private CorsWebFilter corsWebFilter;

    @Autowired
    private GatewayErrorHandler gatewayErrorHandler;

    @Test
    void contextLoads() {
        assertThat(corsWebFilter).isNotNull();
        assertThat(gatewayErrorHandler).isNotNull();
    }

    @Test
    void allDownstreamRoutesAreRegistered() {
        List<String> ids = cinemindRoutes.getRoutes()
                .map(Route::getId)
                .collectList()
                .block();

        assertThat(ids).containsExactlyInAnyOrder(
                "movie-service", "rag-service", "ai-orchestrator");
    }

    @Test
    void routesUseEurekaLogicalNamesInsteadOfHardcodedHosts() {
        List<URI> uris = cinemindRoutes.getRoutes()
                .map(Route::getUri)
                .collectList()
                .block();

        assertThat(uris).hasSize(3)
                .allSatisfy(uri -> assertThat(uri.getScheme()).isEqualTo("lb"));
        assertThat(uris).extracting(URI::getHost)
                .containsExactlyInAnyOrder(
                        "MOVIE-SERVICE", "RAG-SERVICE", "AI-ORCHESTRATOR");
    }

    @Test
    void unavailableDownstreamReturnsStructuredJson503() {
        webTestClient.get().uri("/api/movies")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.path").isEqualTo("/api/movies")
                .jsonPath("$.message").isNotEmpty();
    }

}
