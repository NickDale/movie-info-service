package movieinfo.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import movieinfo.controller.model.MovieListResponse;
import movieinfo.controller.model.Provider;
import movieinfo.service.MovieService;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping(value = "/{movieTitle}", produces = MediaType.APPLICATION_JSON_VALUE)
    public MovieListResponse getMovies(@PathVariable
                                       @NotBlank(message = "Movie title must not be blank")
                                       @Size(min = 1, max = 255, message = "Movie title must be between 1 and 255 characters")
                                       String movieTitle,
                                       @RequestParam(name = "api") Provider provider) {
        log.info("Received movie search request: title='{}', api='{}'", movieTitle, provider);
        return movieService.getMovies(provider, movieTitle);
    }
}
