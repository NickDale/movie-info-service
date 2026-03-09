package movieinfo.service.external;


import lombok.extern.slf4j.Slf4j;
import movieinfo.controller.model.Movie;
import movieinfo.exception.MovieApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class MovieDbApiAdapterTest {

    private static MockWebServer mockWebServer;
    private MovieDbApiAdapter adapter;

    private static String searchUrl;
    private static String detailUrl;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String base = "http://localhost:" + mockWebServer.getPort();
        searchUrl = base + "/search/movie?query={title}&include_adult=true&api_key={key}";
        detailUrl = base + "/movie/{movieId}/credits?api_key={key}";
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() throws Exception {
        RecordedRequest leftover;
        while ((leftover = mockWebServer.takeRequest(0, TimeUnit.MILLISECONDS)) != null) {
            log.debug("Draining leftover request: {}", leftover.getPath());
        }

        RestClient restClient = RestClient.builder()
                .requestFactory(new SimpleClientHttpRequestFactory())
                .build();
        adapter = new MovieDbApiAdapter(restClient, Executors.newFixedThreadPool(4));
        setField("searchUrl", searchUrl);
        setField("detailsUrl", detailUrl);
        setField("apiKey", "test-key");
    }

    @Test
    void searchMoviesByTitle_returnsSingleMovieWithDirector() {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 24428, "title": "The Avengers", "release_date": "2012-05-04"}]
                }
                """);
        enqueueCreditsResponse("""
                {
                  "crew": [
                    {"name": "Joss Whedon", "job": "Director"},
                    {"name": "Some Editor", "job": "Editor"}
                  ]
                }
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Avengers");

        assertThat(result).hasSize(1);
        Movie movie = result.getFirst();
        assertThat(movie.getTitle()).isEqualTo("The Avengers");
        assertThat(movie.getYear()).isEqualTo("2012");
        assertThat(movie.getDirector()).containsExactly("Joss Whedon");
    }

    @Test
    void searchMoviesByTitle_parsesMultipleDirectors() {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 299536, "title": "Avengers: Infinity War", "release_date": "2018-04-27"}]
                }
                """);
        enqueueCreditsResponse("""
                {
                  "crew": [
                    {"name": "Anthony Russo", "job": "Director"},
                    {"name": "Joe Russo", "job": "Director"},
                    {"name": "Someone", "job": "Producer"}
                  ]
                }
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Avengers: Infinity War");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDirector())
                .containsExactlyInAnyOrder("Anthony Russo", "Joe Russo");
    }

    @Test
    void searchMoviesByTitle_returnsEmptyList_whenResultsEmpty() {
        enqueueSearchResponse("""
                {"results": []}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("xyznonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void searchMoviesByTitle_returnsEmptyList_whenResultsNull() {
        enqueueSearchResponse("""
                {}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("something");

        assertThat(result).isEmpty();
    }

    @Test
    void searchMoviesByTitle_returnsNAYear_whenReleaseDateNull() {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 999, "title": "No Date Movie"}]
                }
                """);
        enqueueCreditsResponse("""
                {"crew": [{"name": "John Doe", "job": "Director"}]}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("No Date Movie");

        assertThat(result.getFirst().getYear()).isEqualTo("N/A");
    }

    @Test
    void searchMoviesByTitle_returnsEmptyDirectors_whenCrewIsNull() {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 24428, "title": "The Avengers", "release_date": "2012-05-04"}]
                }
                """);
        enqueueCreditsResponse("""
                {}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Avengers");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDirector()).isEmpty();
    }

    @Test
    void searchMoviesByTitle_returnsEmptyDirectors_whenNoDirectorInCrew() {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 24428, "title": "The Avengers", "release_date": "2012-05-04"}]
                }
                """);
        enqueueCreditsResponse("""
                {
                  "crew": [
                    {"name": "Some Editor", "job": "Editor"},
                    {"name": "Some Producer", "job": "Producer"}
                  ]
                }
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Avengers");

        assertThat(result.getFirst().getDirector()).isEmpty();
    }

    @Test
    void searchMoviesByTitle_throwsMovieApiException_whenSearchRequestFails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> adapter.searchMoviesByTitle("Avengers"))
                .isInstanceOf(MovieApiException.class)
                .hasMessageContaining("Search request failed");
    }

    @Test
    void searchMoviesByTitle_returnsEmptyDirectors_whenCreditsRequestFails() {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 24428, "title": "The Avengers", "release_date": "2012-05-04"}]
                }
                """);
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        List<Movie> result = adapter.searchMoviesByTitle("Avengers");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDirector()).isEmpty();
    }

    @Test
    void searchMoviesByTitle_sendsCorrectSearchRequest() throws InterruptedException {
        enqueueSearchResponse("""
                {"results": []}
                """);

        adapter.searchMoviesByTitle("Inception");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("query=Inception");
        assertThat(request.getPath()).contains("api_key=test-key");
    }

    @Test
    void searchMoviesByTitle_sendsCorrectCreditsRequest() throws InterruptedException {
        enqueueSearchResponse("""
                {
                  "results": [{"id": 27205, "title": "Inception", "release_date": "2010-07-16"}]
                }
                """);
        enqueueCreditsResponse("""
                {"crew": [{"name": "Christopher Nolan", "job": "Director"}]}
                """);

        adapter.searchMoviesByTitle("Inception");

        mockWebServer.takeRequest(); // search request
        RecordedRequest creditsRequest = mockWebServer.takeRequest();
        assertThat(creditsRequest.getPath()).contains("/movie/27205/credits");
        assertThat(creditsRequest.getPath()).contains("api_key=test-key");
    }

    private void enqueueSearchResponse(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));
    }

    private void enqueueCreditsResponse(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = MovieDbApiAdapter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(adapter, value);
    }
}
