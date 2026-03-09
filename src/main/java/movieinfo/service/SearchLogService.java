package movieinfo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieinfo.controller.model.Provider;
import movieinfo.entity.SearchLog;
import movieinfo.repository.SearchLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;

    @Async("searchLogExecutor")
    @Transactional
    public void logSearchFromCache(final Provider provider, String searchTerm, int resultsCount, long responseTimeMs) {
        logSearch(provider, searchTerm, resultsCount, responseTimeMs, true);
    }

    @Async("searchLogExecutor")
    @Transactional
    public void logSearch(Provider provider, String searchTerm, int resultsCount, long responseTimeMs) {
        logSearch(provider, searchTerm, resultsCount, responseTimeMs, false);
    }

    private void logSearch(Provider provider, String searchTerm, int resultsCount, long responseTimeMs, boolean cacheHit) {
        try {
            SearchLog entry = SearchLog.builder()
                    .searchTerm(searchTerm.trim().toLowerCase())
                    .apiName(provider.name())
                    .resultsCount(resultsCount)
                    .cacheHit(cacheHit)
                    .responseTimeMs(responseTimeMs)
                    .searchedAt(LocalDateTime.now())
                    .build();
            searchLogRepository.save(entry);
            log.debug("Search logged: term='{}', api='{}', results={}, cacheHit={}, time={}ms",
                    searchTerm, provider.name(), resultsCount, cacheHit, responseTimeMs);
        } catch (Exception ex) {
            log.error("Failed to persist search log: {}", ex.getMessage(), ex);
        }
    }
}
