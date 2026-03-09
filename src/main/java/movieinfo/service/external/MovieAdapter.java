package movieinfo.service.external;

import movieinfo.service.MovieApiService;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.Executor;

public abstract sealed class MovieAdapter implements MovieApiService permits OmdbApiAdapter, MovieDbApiAdapter {
    protected static final String NO_DATA = "N/A";
    protected final RestClient restClient;
    protected final Executor searchLogExecutor;

    protected MovieAdapter(RestClient restClient, Executor searchLogExecutor) {
        this.restClient = restClient;
        this.searchLogExecutor = searchLogExecutor;
    }

    protected abstract String detailsUrl();

    protected abstract String searchUrl();

    protected abstract String apiKey();

    protected <T> T fetchDetailsByMovieId(final String movieId, Class<T> responseType) {
        return fetch(detailsUrl(), movieId, responseType);
    }

    protected <T> T fetchSearchResultByTitle(final String title, Class<T> responseType) {
        return fetch(searchUrl(), title, responseType);
    }

    protected <T> T fetch(final String url, final String param, Class<T> responseType) {
        return restClient.get()
                .uri(url, param, apiKey())
                .retrieve()
                .body(responseType);
    }

    abstract List<String> fetchDirector(String movieId);
}
