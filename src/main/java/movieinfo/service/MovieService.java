package movieinfo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieinfo.controller.model.MovieListResponse;
import movieinfo.controller.model.Provider;
import movieinfo.service.external.MovieProviderFactory;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieProviderFactory movieProviderFactory;
    private final SearchLogService searchLogService;
    private final CacheService cacheService;

    public MovieListResponse getMovies(final Provider provider, final String title) {
        return cacheService.retrieveFromCache(provider, title)
                .orElseGet(
                        () -> getMoviesFromAPICall(provider, title)
                );
    }

    private MovieListResponse getMoviesFromAPICall(final Provider provider, final String title) {
        final long startTime = System.currentTimeMillis();
        final MovieListResponse response = new MovieListResponse(
                movieProviderFactory.instanceOf(provider).searchMoviesByTitle(title)
        );
        cacheService.cacheMovieList(provider, title, response);
        long elapsed = System.currentTimeMillis() - startTime;
        searchLogService.logSearch(provider, title, response.movies().size(), elapsed);
        return response;
    }

}

