package com.cinemind.rag.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.cinemind.rag.dto.MovieMetadata;
import com.cinemind.rag.dto.PagedMovies;
import com.cinemind.rag.exception.MovieServiceUnavailableException;

/**
 * Reads movie metadata from the Movie Service through Eureka service discovery.
 *
 * IMPORTANT: this class deliberately does NOT declare a RestClient.Builder
 * bean. Declaring one makes Spring Boot's own prototype builder back off and
 * Spring AI's Ollama client would then inherit it (and try to load-balance
 * "localhost"). We use a private plain RestClient plus DiscoveryClient instead.
 */
@Component
public class MovieServiceClient {

    private static final String SERVICE_ID = "MOVIE-SERVICE";
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 100;

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient = RestClient.builder().build();

    public MovieServiceClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    public Optional<MovieMetadata> getMovie(long movieId) {
        try {
            MovieMetadata movie = restClient.get()
                    .uri(baseUrl() + "/api/movies/{id}", movieId)
                    .retrieve()
                    .body(MovieMetadata.class);
            return Optional.ofNullable(movie);
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw unavailable(ex);
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    public List<MovieMetadata> getAllMovies() {
        List<MovieMetadata> movies = new ArrayList<>();
        String base = baseUrl();
        for (int page = 0; page < MAX_PAGES; page++) {
            final int currentPage = page;
            PagedMovies response;
            try {
                response = restClient.get()
                        .uri(base + "/api/movies?page={page}&size={size}",
                                currentPage, PAGE_SIZE)
                        .retrieve()
                        .body(PagedMovies.class);
            } catch (RestClientException ex) {
                throw unavailable(ex);
            }
            if (response == null || response.getContent().isEmpty()) {
                break;
            }
            movies.addAll(response.getContent());
            if (response.isLast()) {
                break;
            }
        }
        return movies;
    }

    /** Resolves the current Movie Service instance via Eureka - no hardcoded host. */
    private String baseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(SERVICE_ID);
        if (instances.isEmpty()) {
            throw new MovieServiceUnavailableException(
                    "No '" + SERVICE_ID + "' instance is registered in Eureka");
        }
        return instances.get(0).getUri().toString();
    }

    private static MovieServiceUnavailableException unavailable(Throwable cause) {
        return new MovieServiceUnavailableException(
                "Movie Service is unavailable (is MOVIE-SERVICE registered in Eureka?)", cause);
    }
}
