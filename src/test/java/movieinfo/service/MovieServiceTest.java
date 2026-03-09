package movieinfo.service;

import movieinfo.controller.model.Movie;
import movieinfo.controller.model.MovieListResponse;
import movieinfo.controller.model.Provider;
import movieinfo.service.external.MovieProviderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieProviderFactory movieProviderFactory;
    @Mock
    private SearchLogService searchLogService;
    @Mock
    private CacheService cacheService;
    @Mock
    private MovieApiService movieApiService;

    @InjectMocks
    private MovieService movieService;

    private final List<Movie> sampleMovies = List.of(
            Movie.builder().title("Avengers").year("2012").director(List.of("Joss Whedon")).build()
    );

    @Test
    void getMovies_returnsCachedResult_whenCacheHit() {
        MovieListResponse cached = new MovieListResponse(sampleMovies);
        when(cacheService.retrieveFromCache(Provider.OMDB, "Avengers")).thenReturn(Optional.of(cached));

        MovieListResponse result = movieService.getMovies(Provider.OMDB, "Avengers");

        assertThat(result.movies()).isEqualTo(sampleMovies);
        verifyNoInteractions(movieProviderFactory, movieApiService);
    }

    @Test
    void getMovies_callsApi_whenCacheMiss() {
        when(cacheService.retrieveFromCache(Provider.OMDB, "Avengers")).thenReturn(Optional.empty());
        when(movieApiService.searchMoviesByTitle("Avengers")).thenReturn(sampleMovies);
        when(movieProviderFactory.instanceOf(any())).thenReturn(movieApiService);

        MovieListResponse result = movieService.getMovies(Provider.OMDB, "Avengers");

        assertThat(result.movies()).isEqualTo(sampleMovies);
        verify(movieApiService).searchMoviesByTitle("Avengers");
    }

    @Test
    void getMovies_storesResultInCache_afterApiCall() {
        when(cacheService.retrieveFromCache(Provider.OMDB, "Avengers")).thenReturn(Optional.empty());
        when(movieApiService.searchMoviesByTitle("Avengers")).thenReturn(sampleMovies);
        when(movieProviderFactory.instanceOf(any())).thenReturn(movieApiService);

        movieService.getMovies(Provider.OMDB, "Avengers");

        verify(cacheService).cacheMovieList(eq(Provider.OMDB), eq("Avengers"), any(MovieListResponse.class));
    }

    @Test
    void getMovies_logsSearch_afterApiCall() {
        when(cacheService.retrieveFromCache(Provider.TMDB, "Inception")).thenReturn(Optional.empty());
        when(movieApiService.searchMoviesByTitle("Inception")).thenReturn(sampleMovies);
        when(movieProviderFactory.instanceOf(any())).thenReturn(movieApiService);

        movieService.getMovies(Provider.TMDB, "Inception");

        verify(searchLogService).logSearch(eq(Provider.TMDB), eq("Inception"), eq(1), anyLong());
    }

    @Test
    void getMovies_doesNotLogSearch_whenCacheHit() {
        when(cacheService.retrieveFromCache(Provider.OMDB, "Avengers"))
                .thenReturn(Optional.of(new MovieListResponse(sampleMovies)));

        movieService.getMovies(Provider.OMDB, "Avengers");

        verifyNoInteractions(searchLogService);
    }

    @Test
    void getMovies_routesToCorrectProvider() {
        when(cacheService.retrieveFromCache(Provider.TMDB, "Inception")).thenReturn(Optional.empty());
        when(movieApiService.searchMoviesByTitle("Inception")).thenReturn(List.of());
        when(movieProviderFactory.instanceOf(any())).thenReturn(movieApiService);

        movieService.getMovies(Provider.TMDB, "Inception");

        verify(movieProviderFactory).instanceOf(Provider.TMDB);
    }

    @Test
    void getMovies_returnsEmptyList_whenApiReturnsNoResults() {
        when(cacheService.retrieveFromCache(any(), any())).thenReturn(Optional.empty());
        when(movieApiService.searchMoviesByTitle(any())).thenReturn(List.of());
        when(movieProviderFactory.instanceOf(any())).thenReturn(movieApiService);

        MovieListResponse result = movieService.getMovies(Provider.OMDB, "xyznonexistent");

        assertThat(result.movies()).isEmpty();
    }
}
