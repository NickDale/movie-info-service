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
class OmdbApiAdapterTest {
    private static MockWebServer mockWebServer;
    private OmdbApiAdapter adapter;

    private static String searchUrl;
    private static String detailUrl;

    @BeforeAll
    static void startServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        String base = "http://localhost:" + mockWebServer.getPort();
        searchUrl = base + "/?s={title}&apikey={key}";
        detailUrl = base + "/?i={movieId}&apikey={key}";
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
        adapter = new OmdbApiAdapter(restClient, Executors.newFixedThreadPool(4));
        setField("searchUrl", searchUrl);
        setField("detailsUrl", detailUrl);
        setField("apiKey", "test-key");
    }

    @Test
    void searchMoviesByTitle_returnsSingleMovieWithDirector() {
        enqueueSearchResponse("""
                {
                  "Search": [{"imdbID": "tt0123456", "Title": "Avengers", "Year": "2012"}],
                  "Response": "True"
                }
                """);
        enqueueDetailResponse("""
                {"Title": "Avengers", "Year": "2012", "Director": "Joss Whedon", "Response": "True"}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Avengers");

        assertThat(result).hasSize(1);
        Movie movie = result.getFirst();
        assertThat(movie.getTitle()).isEqualTo("Avengers");
        assertThat(movie.getYear()).isEqualTo("2012");
        assertThat(movie.getDirector()).containsExactly("Joss Whedon");
    }

    @Test
    void searchMoviesByTitle_parsesMultipleDirectors() {
        enqueueSearchResponse("""
                {
                  "Search": [{"imdbID": "tt0654321", "Title": "Avengers: Endgame", "Year": "2019"}],
                  "Response": "True"
                }
                """);
        enqueueDetailResponse("""
                {"Title": "Avengers: Endgame", "Year": "2019", "Director": "Anthony Russo, Joe Russo", "Response": "True"}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Avengers: Endgame");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDirector())
                .containsExactlyInAnyOrder("Anthony Russo", "Joe Russo");
    }

    @Test
    void searchMoviesByTitle_returnsEmptyList_whenResponseFalse() {
        enqueueSearchResponse("""
                {"Response": "False", "Error": "Movie not found!"}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("xyznonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void searchMoviesByTitle_returnsEmptyList_whenSearchIsNull() {
        enqueueSearchResponse("""
                {"Response": "True"}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("something");

        assertThat(result).isEmpty();
    }

    @Test
    void searchMoviesByTitle_returnsMovieWithEmptyDirectors_whenDetailResponseFalse() {
        enqueueSearchResponse("""
                {
                  "Search": [{"imdbID": "tt9999999", "Title": "Unknown", "Year": "2000"}],
                  "Response": "True"
                }
                """);
        enqueueDetailResponse("""
                {"Response": "False"}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Unknown");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getDirector()).isEmpty();
    }

    @Test
    void searchMoviesByTitle_returnsEmptyDirectors_whenDirectorIsNA() {
        enqueueSearchResponse("""
                {
                  "Search": [{"imdbID": "tt1111111", "Title": "Some Movie", "Year": "2005"}],
                  "Response": "True"
                }
                """);
        enqueueDetailResponse("""
                {"Title": "Some Movie", "Year": "2005", "Director": "N/A", "Response": "True"}
                """);

        List<Movie> result = adapter.searchMoviesByTitle("Some Movie");

        assertThat(result.get(0).getDirector()).isEmpty();
    }

    @Test
    void searchMoviesByTitle_throwsMovieApiException_whenSearchRequestFails() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> adapter.searchMoviesByTitle("Avengers"))
                .isInstanceOf(MovieApiException.class)
                .hasMessageContaining("Search request failed");
    }

    @Test
    void searchMoviesByTitle_returnsMovieWithEmptyDirectors_whenDetailRequestFails() {
        enqueueSearchResponse("""
                {
                  "Search": [{"imdbID": "tt0123456", "Title": "Avengers", "Year": "2012"}],
                  "Response": "True"
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
                {"Response": "False"}
                """);

        adapter.searchMoviesByTitle("Inception");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("s=Inception");
        assertThat(request.getPath()).contains("apikey=test-key");
    }

    @Test
    void searchMoviesByTitle_sendsCorrectDetailRequest() throws InterruptedException {
        enqueueSearchResponse("""
                {
                  "Search": [{"imdbID": "tt1375666", "Title": "Inception", "Year": "2010"}],
                  "Response": "True"
                }
                """);
        enqueueDetailResponse("""
                {"Title": "Inception", "Year": "2010", "Director": "Christopher Nolan", "Response": "True"}
                """);

        adapter.searchMoviesByTitle("Inception");

        mockWebServer.takeRequest();
        RecordedRequest detailRequest = mockWebServer.takeRequest();
        assertThat(detailRequest.getPath()).contains("i=tt1375666");
        assertThat(detailRequest.getPath()).contains("apikey=test-key");
    }

    private void enqueueSearchResponse(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));
    }

    private void enqueueDetailResponse(String body) {
        mockWebServer.enqueue(new MockResponse()
                .setBody(body)
                .addHeader("Content-Type", "application/json"));
    }

    private void setField(String fieldName, Object value) throws Exception {
        Field field = OmdbApiAdapter.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(adapter, value);
    }
}