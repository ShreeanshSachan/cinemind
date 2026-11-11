package com.cinemind.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** Two Gemini Flash agents coordinated by a deterministic orchestrator (port 8084). */
@SpringBootApplication
@EnableDiscoveryClient
public class AiOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiOrchestratorApplication.class, args);
    }
}
