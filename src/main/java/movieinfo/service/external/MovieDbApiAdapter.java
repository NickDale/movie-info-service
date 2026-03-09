package movieinfo.service.external;

import lombok.extern.slf4j.Slf4j;
import movieinfo.controller.model.Movie;
import movieinfo.exception.MovieApiException;
import movieinfo.service.external.model.MovieDbResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

@Slf4j
@Component
final class MovieDbApiAdapter extends MovieAdapter {


    private final BiFunction<MovieDbResponse.MovieResult, List<String>, Movie> movieMapper =
            (movie, directors) -> Movie.builder()
                    .title(movie.title())
                    .year(Optional.ofNullable(movie.releaseDate())
                            .map(rd -> String.valueOf(rd.getYear()))
                            .orElse(NO_DATA)
                    )
                    .director(directors)
                    .build();

    @Value("${movie.api.tmdb.search-url}")
    private String searchUrl;
    @Value("${movie.api.tmdb.detail-url}")
    private String detailsUrl;
    @Value("${movie.api.tmdb.api-key}")
    private String apiKey;

    MovieDbApiAdapter(RestClient restClient, Executor searchLogExecutor) {
        super(restClient, searchLogExecutor);
    }

    @Override
    protected String detailsUrl() {
        return this.detailsUrl;
    }

    @Override
    protected String searchUrl() {
        return this.searchUrl;
    }

    @Override
    protected String apiKey() {
        return this.apiKey;
    }

    @Override
    public List<Movie> searchMoviesByTitle(String title) {
        List<MovieDbResponse.MovieResult> searchResults = fetchSearchResults(title);
        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        return searchResults.stream()
                .map(movie ->
                        CompletableFuture.supplyAsync(() -> fetchDirector(String.valueOf(movie.id())), searchLogExecutor)
                                .thenApply(directors -> movieMapper.apply(movie, directors))
                )
                .map(CompletableFuture::join)
                .toList();
    }

    private List<MovieDbResponse.MovieResult> fetchSearchResults(String title) {
        try {
            MovieDbResponse.SearchResponse response = fetchSearchResultByTitle(title, MovieDbResponse.SearchResponse.class);

            if (response == null || response.results() == null) {
                return Collections.emptyList();
            }
            return response.results();
        } catch (RestClientException e) {
            throw new MovieApiException("MovieDbApiAdapter", "Search request failed: " + e.getMessage(), e);
        }
    }

    @Override
    List<String> fetchDirector(String movieId) {
        try {
            MovieDbResponse.DetailsResponse movieDetails = fetchDetailsByMovieId(movieId, MovieDbResponse.DetailsResponse.class);
            if (movieDetails == null || movieDetails.crew() == null) {
                return Collections.emptyList();
            }
            return movieDetails.crew().stream()
                    .filter(m -> "Director".equalsIgnoreCase(m.job()))
                    .map(MovieDbResponse.CrewMember::name)
                    .toList();
        } catch (RestClientException e) {
            log.warn("MovieDbApiAdapter: credits fetch failed for movieId={}: {}", movieId, e.getMessage());
            return Collections.emptyList();
        }
    }

}
