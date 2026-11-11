package com.cinemind.movie.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.cinemind.movie.dto.MovieFilter;
import com.cinemind.movie.dto.MovieRequest;
import com.cinemind.movie.dto.MovieResponse;
import com.cinemind.movie.dto.PagedResponse;
import com.cinemind.movie.exception.MovieNotFoundException;
import com.cinemind.movie.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovieService movieService;

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(movieService.create(any(MovieRequest.class))).thenReturn(response(1L, "Inception"));

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Inception", 2010))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/movies/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    void createRejectsBlankTitleWith400() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void createRejectsOutOfRangeRating() throws Exception {
        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"rating\":11.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        PagedResponse<MovieResponse> page = new PagedResponse<>(
                new PageImpl<>(List.of(response(1L, "Inception")), PageRequest.of(0, 5), 1));
        when(movieService.list(0, 5, "id", "asc")).thenReturn(page);

        mockMvc.perform(get("/api/movies").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Inception"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(movieService.getById(99L)).thenThrow(new MovieNotFoundException(99L));

        mockMvc.perform(get("/api/movies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Movie with id 99 was not found"));
    }

    @Test
    void getByIdReturns400ForNonNumericId() throws Exception {
        mockMvc.perform(get("/api/movies/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void updateReturns200() throws Exception {
        when(movieService.update(eq(1L), any(MovieRequest.class)))
                .thenReturn(response(1L, "Inception"));

        mockMvc.perform(put("/api/movies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request("Inception", 2010))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isNoContent());

        verify(movieService).delete(1L);
    }

    @Test
    void searchRequiresTitleParameter() throws Exception {
        mockMvc.perform(get("/api/movies/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchDelegatesTitleToService() throws Exception {
        PagedResponse<MovieResponse> empty = new PagedResponse<>(new PageImpl<>(List.of()));
        when(movieService.search("Inception", 0, 20, "id", "asc")).thenReturn(empty);

        mockMvc.perform(get("/api/movies/search").param("title", "Inception"))
                .andExpect(status().isOk());

        verify(movieService).search("Inception", 0, 20, "id", "asc");
    }

    @Test
    void filterPassesQueryParametersToService() throws Exception {
        PagedResponse<MovieResponse> empty = new PagedResponse<>(new PageImpl<>(List.of()));
        when(movieService.filter(any(MovieFilter.class), eq(0), eq(20), eq("id"), eq("asc")))
                .thenReturn(empty);

        mockMvc.perform(get("/api/movies/filter")
                        .param("genre", "Thriller")
                        .param("yearFrom", "2010")
                        .param("maxRuntime", "140"))
                .andExpect(status().isOk());

        ArgumentCaptor<MovieFilter> captor = ArgumentCaptor.forClass(MovieFilter.class);
        verify(movieService).filter(captor.capture(), eq(0), eq(20), eq("id"), eq("asc"));
        assertThat(captor.getValue().getGenre()).isEqualTo("Thriller");
        assertThat(captor.getValue().getYearFrom()).isEqualTo(2010);
        assertThat(captor.getValue().getMaxRuntime()).isEqualTo(140);
    }

    private static MovieRequest request(String title, Integer year) {
        MovieRequest request = new MovieRequest();
        request.setTitle(title);
        request.setReleaseYear(year);
        request.setGenres(List.of("Science Fiction", "Thriller"));
        request.setRuntimeMinutes(148);
        return request;
    }

    private static MovieResponse response(Long id, String title) {
        MovieResponse response = new MovieResponse();
        response.setId(id);
        response.setTitle(title);
        response.setGenres(List.of("Science Fiction", "Thriller"));
        return response;
    }
}
