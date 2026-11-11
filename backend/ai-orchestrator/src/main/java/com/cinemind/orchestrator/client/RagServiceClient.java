package com.cinemind.orchestrator.client;

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.cinemind.orchestrator.dto.rag.RagSearchRequest;
import com.cinemind.orchestrator.dto.rag.RagSearchResponse;
import com.cinemind.orchestrator.exception.RagUnavailableException;

/**
 * Calls the RAG Service through Eureka discovery. No RestClient.Builder bean is
 * declared (that would replace Spring Boot's prototype builder and affect
 * Spring AI's own HTTP client); a private plain RestClient is used instead.
 */
@Component
public class RagServiceClient {

    private static final String SERVICE_ID = "RAG-SERVICE";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient = RestClient.builder().build();

    public RagServiceClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public RagSearchResponse search(RagSearchRequest request) {
        try {
            RagSearchResponse response = restClient.post()
                    .uri(baseUrl() + "/api/rag/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(RagSearchResponse.class);
            if (response == null) {
                throw new RagUnavailableException("RAG Service returned an empty response");
            }
            return response;
        } catch (RestClientException ex) {
            throw new RagUnavailableException(
                    "RAG Service is unavailable (is RAG-SERVICE registered in Eureka?)", ex);
        }
    }

    private String baseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_ID);
        if (instances.isEmpty()) {
            throw new RagUnavailableException(
                    "No '" + SERVICE_ID + "' instance is registered in Eureka");
        }
        return instances.get(0).getUri().toString();
    }
}
