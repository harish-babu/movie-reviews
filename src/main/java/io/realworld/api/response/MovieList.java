package io.realworld.api.response;

import java.util.List;

public class MovieList {
    private List<Movie> movies;
    private int moviesCount;

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(final List<Movie> movies) {
        this.movies = movies;
    }

    public int getMoviesCount() {
        return moviesCount;
    }

    public void setMoviesCount(final int moviesCount) {
        this.moviesCount = moviesCount;
    }
}
