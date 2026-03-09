package movieinfo.service.external;

import lombok.RequiredArgsConstructor;
import movieinfo.controller.model.Provider;
import movieinfo.service.MovieApiService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovieProviderFactory {
    private final MovieDbApiAdapter movieDbApiAdapter;
    private final OmdbApiAdapter omdbApiAdapter;

    public MovieApiService instanceOf(final Provider provider) {
        return switch (provider) {
            case TMDB -> movieDbApiAdapter;
            case OMDB -> omdbApiAdapter;
        };
    }
}
