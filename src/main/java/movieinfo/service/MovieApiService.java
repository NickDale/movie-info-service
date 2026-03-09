package movieinfo.service;


import movieinfo.controller.model.Movie;

import java.util.List;

public interface MovieApiService {

    List<Movie> searchMoviesByTitle(String title);

}
