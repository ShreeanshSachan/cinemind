package com.cinemind.movie;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.cinemind.movie.dto.MovieRequest;
import com.cinemind.movie.dto.MovieResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Full-context test: real HTTP, real service, real repository, in-memory H2. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MovieServiceApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
        assertThat(restTemplate).isNotNull();
    }

    @Test
    void fullCrudRoundTripOverHttp() throws Exception {
        MovieRequest request = request("Integration Round Trip", 2020);

        ResponseEntity<MovieResponse> created =
                restTemplate.postForEntity("/api/movies", request, MovieResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        Long id = created.getBody().getId();
        assertThat(id).isNotNull();
        assertThat(created.getBody().getGenres()).containsExactly("Drama", "Thriller");

        ResponseEntity<MovieResponse> fetched =
                restTemplate.getForEntity("/api/movies/" + id, MovieResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().getTitle()).isEqualTo("Integration Round Trip");

        request.setTitle("Integration Round Trip Updated");
        ResponseEntity<MovieResponse> updated = restTemplate.exchange(
                "/api/movies/" + id, HttpMethod.PUT,
                new HttpEntity<>(request), MovieResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody().getTitle()).isEqualTo("Integration Round Trip Updated");

        ResponseEntity<String> listing =
                restTemplate.getForEntity("/api/movies?page=0&size=5", String.class);
        assertThat(listing.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(listing.getBody());
        assertThat(body.get("size").asInt()).isEqualTo(5);
        assertThat(body.get("content").isArray()).isTrue();

        restTemplate.delete("/api/movies/" + id);
        ResponseEntity<String> afterDelete =
                restTemplate.getForEntity("/api/movies/" + id, String.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void duplicateTitleAndYearReturns409() {
        MovieRequest request = request("Duplicate Probe", 2019);
        ResponseEntity<MovieResponse> first =
                restTemplate.postForEntity("/api/movies", request, MovieResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> second =
                restTemplate.postForEntity("/api/movies", request, String.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        restTemplate.delete("/api/movies/" + first.getBody().getId());
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/movies",
                new HttpEntity<>("{\"title\":\"\",\"rating\":11.0}", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("status").asInt()).isEqualTo(400);
        assertThat(body.get("fieldErrors").has("title")).isTrue();
        assertThat(body.get("fieldErrors").has("rating")).isTrue();
    }

    @Test
    void unsupportedMediaTypeReturns415Not500() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/movies",
                new HttpEntity<>("plain text is not a movie", headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(objectMapper.readTree(response.getBody()).get("status").asInt()).isEqualTo(415);
    }

    @Test
    void filterEndpointReturnsMatchingMovies() throws Exception {
        MovieRequest request = request("Filter Probe Thriller", 2012);
        request.setGenres(List.of("Thriller"));
        request.setRuntimeMinutes(120);
        ResponseEntity<MovieResponse> created =
                restTemplate.postForEntity("/api/movies", request, MovieResponse.class);

        ResponseEntity<String> filtered = restTemplate.getForEntity(
                "/api/movies/filter?genre=Thriller&yearFrom=2012&yearTo=2012&maxRuntime=130",
                String.class);
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(filtered.getBody());
        assertThat(body.get("content").toString()).contains("Filter Probe Thriller");

        restTemplate.delete("/api/movies/" + created.getBody().getId());
    }

    private static MovieRequest request(String title, Integer year) {
        MovieRequest request = new MovieRequest();
        request.setTitle(title);
        request.setReleaseYear(year);
        request.setOverview("Created by an integration test.");
        request.setGenres(List.of("Drama", "Thriller"));
        request.setKeywords(List.of("test"));
        request.setRuntimeMinutes(115);
        request.setRating(new BigDecimal("7.5"));
        return request;
    }
}
