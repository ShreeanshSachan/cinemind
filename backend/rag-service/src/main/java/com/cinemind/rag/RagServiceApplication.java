package com.cinemind.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** RAG Service: Ollama embeddings + MySQL + Java cosine similarity (port 8083). */
@SpringBootApplication
@EnableDiscoveryClient
public class RagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}
