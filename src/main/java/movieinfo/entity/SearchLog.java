package movieinfo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_log", indexes = {
    @Index(name = "idx_search_term", columnList = "search_term"),
    @Index(name = "idx_api_name", columnList = "api_name"),
    @Index(name = "idx_searched_at", columnList = "searched_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "search_term", nullable = false, length = 512)
    private String searchTerm;

    @Column(name = "api_name", nullable = false, length = 50)
    private String apiName;

    @Column(name = "results_count")
    private Integer resultsCount;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "searched_at", nullable = false)
    private LocalDateTime searchedAt;

    @PrePersist
    protected void onCreate() {
        if (searchedAt == null) {
            searchedAt = LocalDateTime.now();
        }
    }
}
