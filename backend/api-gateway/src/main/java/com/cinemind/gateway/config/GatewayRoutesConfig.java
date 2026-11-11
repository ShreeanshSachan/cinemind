package com.cinemind.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The three downstream routes. Service names are Eureka application names,
 * resolved by Spring Cloud LoadBalancer at request time.
 *
 * Declaring routes in Java (instead of YAML) keeps this service independent of
 * Spring Cloud Gateway property-prefix changes between releases.
 */
@Configuration
public class GatewayRoutesConfig {

    public static final String MOVIE_SERVICE = "lb://MOVIE-SERVICE";
    public static final String RAG_SERVICE = "lb://RAG-SERVICE";
    public static final String AI_ORCHESTRATOR = "lb://AI-ORCHESTRATOR";

    @Bean
    public RouteLocator cinemindRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("movie-service", r -> r.path("/api/movies/**").uri(MOVIE_SERVICE))
                .route("rag-service", r -> r.path("/api/rag/**").uri(RAG_SERVICE))
                .route("ai-orchestrator", r -> r.path("/api/recommendations/**").uri(AI_ORCHESTRATOR))
                .build();
    }
}
