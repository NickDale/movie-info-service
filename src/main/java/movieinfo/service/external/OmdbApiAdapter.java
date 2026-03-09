package movieinfo.service.external;

import lombok.extern.slf4j.Slf4j;
import movieinfo.controller.model.Movie;
import movieinfo.exception.MovieApiException;
import movieinfo.service.external.model.ObdbApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Component
final class OmdbApiAdapter extends MovieAdapter {

    private final BiFunction<ObdbApiResponse.SearchResult, List<String>, Movie> movieMapper =
            (searchResult, directors) -> Movie.builder()
                    .title(searchResult.title())
                    .year(searchResult.year())
                    .director(directors)
                    .build();

    @Value("${movie.api.omdb.search-url}")
    private String searchUrl;
    @Value("${movie.api.omdb.detail-url}")
    private String detailsUrl;
    @Value("${movie.api.omdb.api-key}")
    private String apiKey;

    OmdbApiAdapter(RestClient restClient, Executor searchLogExecutor) {
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
        List<ObdbApiResponse.SearchResult> searchResults = fetchSearchResults(title);
        if (searchResults.isEmpty()) {
            return Collections.emptyList();
        }

        return searchResults.stream()
                .map(searchResult ->
                        CompletableFuture.supplyAsync(() -> fetchDirector(searchResult.imdbId()), searchLogExecutor)
                                .thenApply(directors -> movieMapper.apply(searchResult, directors))
                )
                .map(CompletableFuture::join)
                .toList();
    }

    private List<ObdbApiResponse.SearchResult> fetchSearchResults(String title) {
        try {
            ObdbApiResponse.SearchResponse response = fetchSearchResultByTitle(title, ObdbApiResponse.SearchResponse.class);
            if (response == null || !Boolean.parseBoolean(response.response()) || isEmpty(response.search())) {
                log.debug("OmdbApiAdapter: no results for title='{}'", title);
                return Collections.emptyList();
            }
            return response.search();
        } catch (RestClientException e) {
            throw new MovieApiException("OmdbApiAdapter", "Search request failed: " + e.getMessage(), e);
        }
    }

    @Override
    List<String> fetchDirector(final String imdbId) {
        try {
            final ObdbApiResponse.DetailResponse detail = fetchDetailsByMovieId(imdbId, ObdbApiResponse.DetailResponse.class);
            if (detail == null || !Boolean.parseBoolean(detail.response())) {
                return Collections.emptyList();
            }
            return parseDirectors(detail.director());
        } catch (RestClientException e) {
            log.warn("OmdbApiAdapter: detail fetch failed for imdbID={}: {}", imdbId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> parseDirectors(String directorField) {
        if (directorField == null || directorField.isBlank()
                || NO_DATA.equalsIgnoreCase(directorField)) {
            return Collections.emptyList();
        }
        return Arrays.stream(directorField.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

}
