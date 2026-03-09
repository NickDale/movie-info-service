package movieinfo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieinfo.config.AppConfig;
import movieinfo.controller.model.MovieListResponse;
import movieinfo.controller.model.Provider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {
    private final CacheManager cacheManager;
    private final SearchLogService searchLogService;

    public String getCacheKey(final Provider provider, final String title) {
        final String normalizedTitle = title.trim().toLowerCase();
        return provider.name() + ":" + normalizedTitle;
    }

    public Optional<MovieListResponse> retrieveFromCache(final Provider provider, final String title) {
        final String cacheKey = getCacheKey(provider, title);

        final Cache cache = cacheManager.getCache(AppConfig.MOVIES_CACHE);
        if (cache == null) {
            log.debug("Cache MISS for key='{}', calling {} API", cacheKey, provider.name());
            return Optional.empty();
        }
        final Cache.ValueWrapper cached = cache.get(cacheKey);
        if (cached != null) {
            final long startTime = System.currentTimeMillis();
            log.debug("Cache HIT for key='{}'", cacheKey);
            MovieListResponse result = (MovieListResponse) cached.get();
            int count = result != null && result.movies() != null ? result.movies().size() : 0;

            searchLogService.logSearchFromCache(provider, title, count, System.currentTimeMillis() - startTime);
            return Optional.ofNullable(result);
        }
        log.debug("Cache MISS for key='{}', calling {} API", cacheKey, provider.name());
        return Optional.empty();
    }

    public void cacheMovieList(final Provider provider, final String title, final MovieListResponse response) {
        cacheMovieList(getCacheKey(provider, title), response);
    }

    public void cacheMovieList(final String cacheKey, final MovieListResponse response) {
        final Cache cache = cacheManager.getCache(AppConfig.MOVIES_CACHE);
        if (cache != null && response != null) {
            cache.put(cacheKey, response);
        }
    }

}
