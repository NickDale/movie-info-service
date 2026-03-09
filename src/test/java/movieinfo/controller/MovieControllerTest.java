package movieinfo.controller;

import movieinfo.controller.model.Movie;
import movieinfo.controller.model.MovieListResponse;
import movieinfo.controller.model.Provider;
import movieinfo.exception.GlobalExceptionHandler;
import movieinfo.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
@Import(GlobalExceptionHandler.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    void getMovies_returns200_withValidRequest() throws Exception {
        final String title = "Avengers";
        final String year = "2012";
        final Provider provider = Provider.OMDB;
        final MovieListResponse response = new MovieListResponse(
                List.of(
                        Movie.builder()
                                .title(title)
                                .year(year)
                                .director(
                                        List.of("Joss Whedon")
                                )
                                .build()
                )
        );

        when(movieService.getMovies(eq(provider), eq(title))).thenReturn(response);

        mockMvc.perform(
                        get("/movies/" + title)
                                .param("api", "omdb")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.movies").isArray())
                .andExpect(jsonPath("$.movies[0].Title").value(title))
                .andExpect(jsonPath("$.movies[0].Year").value(year))
                .andExpect(jsonPath("$.movies[0].Director[0]").value("Joss Whedon"));
    }

    @Test
    void getMovies_returns400_withInvalidApiParam() throws Exception {
        mockMvc.perform(
                        get("/movies/Avengers")
                                .param("api", "invalidApi")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMovies_returns400_whenApiParamMissing() throws Exception {
        mockMvc.perform(
                        get("/movies/Avengers").accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMovies_returnsTmdbResults() throws Exception {
        final Provider provider = Provider.TMDB;
        final String title = "Inception";
        final String year = "2010";
        final String director = "Christopher Nolan";
        final MovieListResponse response = new MovieListResponse(
                List.of(
                        Movie.builder()
                                .title(title)
                                .year(year)
                                .director(
                                        List.of(director)
                                )
                                .build()
                )
        );

        when(movieService.getMovies(eq(provider), eq(title))).thenReturn(response);

        mockMvc.perform(
                        get("/movies/Inception")
                                .param("api", "tmdb")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies[0].Title").value(title))
                .andExpect(jsonPath("$.movies[0].Director[0]").value(director));
    }

    @Test
    void getMovies_returnsEmptyList_whenNoMoviesFound() throws Exception {
        when(movieService.getMovies(any(), any())).thenReturn(new MovieListResponse(List.of()));

        mockMvc.perform(
                        get("/movies/xyznonexistent")
                                .param("api", "omdb")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movies").isArray())
                .andExpect(jsonPath("$.movies").isEmpty());
    }
}
