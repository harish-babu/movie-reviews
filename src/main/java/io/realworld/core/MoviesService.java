package io.realworld.core;

import com.google.common.collect.Sets;
import io.realworld.api.request.NewMovie;
import io.realworld.api.response.Movie;
import io.realworld.api.response.MovieList;
import io.realworld.db.*;
import io.realworld.db.mapper.MovieActorsMapper;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.*;

import static java.util.stream.Collectors.*;

public class MoviesService {

    private final UserRepository userRepository;
    private final MoviesRepository moviesRepository;
    private final ActorRepository actorRepository;

    public MoviesService( final UserRepository userRepository,
                          final MoviesRepository moviesRepository, final ActorRepository actorRepository) {
        this.userRepository = userRepository;
        this.moviesRepository = moviesRepository;
        this.actorRepository = actorRepository;
    }

    @Transaction
    public Movie createMovie (final String username, final NewMovie newMovie) {
        final Long movieID = moviesRepository.saveMovie(newMovie.getTitle(),
                newMovie.getDescription(),
                newMovie.getBody(),
                newMovie.getYearReleased(),
                newMovie.getLanguages());

        if (newMovie.getActorList() != null) {
            updateMovieActors(movieID, newMovie.getActorList());
        }

        return findMovieDetails(username, movieID);
    }

    public Movie starMovie(final String username, final String movieId) {
        final Long userId = userRepository.findUserIdByUsername(username);
        final Long articleId = Long.parseLong(movieId);

//        transactionalMovieRepository.likeMovie(userId, articleId);

        moviesRepository.likeMovie(userId, articleId);
        moviesRepository.incrementFavoritesCount(articleId);
        return findMovieDetails(username, articleId);
    }

    @Transaction
    public Movie unStarMovie(final String username, final String movieId) {
        final Long userId = userRepository.findUserIdByUsername(username);
        final Long articleId = Long.parseLong(movieId);

        moviesRepository.unLikeMovie(userId, articleId);
        moviesRepository.decrementFavoritesCount(articleId);
        return findMovieDetails(username, articleId);
    }

    public Movie findById(final String username, final String id) {
        final Movie movie = moviesRepository.findMovieById(Long.parseLong(id));
        fillAdditionalData(List.of(movie), username);
        return movie;
    }
    public MovieList findMovies(final String username,
                                final String author,
                                final String tag,
                                final String favorite,
                                final Integer offset,
                                final int limit) {
        final Long favoriteBy = favorite != null ? userRepository.findUserIdByUsername(favorite) : null;
        final int count = moviesRepository.countMovies(author, tag, favoriteBy);
        final List<Movie> movies = moviesRepository.findMovies(author, tag, favoriteBy, offset, limit);
        fillAdditionalData(movies, username);
        return movieList(movies, count);
    }


    private Movie findMovieDetails (final String username, final Long movieId) {
        final Movie movie = moviesRepository.findMovieById(movieId);
        fillAdditionalData(List.of(movie), username);
        return movie;
    }


    private void fillAdditionalData(final List<Movie> movieList, final String username) {
        final Set<Long> movieIds = movieList.stream().map(Movie::getId).collect(toSet());
        fillActors(movieList, movieIds);
        fillLikes(movieList, username, movieIds);
    }

    private void updateMovieActors (final long movieId, final Set<String> actors) {
        final Set<String> existingActors = actorRepository.findActors(actors);
        final Set<String> missingTags = Sets.difference(actors, existingActors);
        actorRepository.saveActors(missingTags);
        actorRepository.addActorsToMovies(Collections.nCopies(actors.size(), movieId), actors);
    }

    private void fillActors(final List<Movie> movieList, final Set<Long> movieIds) {
        final Map<Long, Set<String>> movieActors = findMovieActors(movieIds);
        for (final Movie movie : movieList) {
            movie.setActorList(movieActors.get(movie.getId()));
        }
    }

    private void fillLikes(final List<Movie> movieList, final String username, final Set<Long> movieIds) {
        if (username != null && movieIds != null && !movieIds.isEmpty()) {
            final Set<Long> favouriteMovieIds = moviesRepository.findFavouriteMovies(username, movieIds);
            for (final Movie movie : movieList) {
                movie.setLiked(favouriteMovieIds.contains(movie.getId()));
            }
        }
    }

    private MovieList movieList(final List<Movie> movies, final int count) {
        final MovieList movieList = new MovieList();
        movieList.setMovies(movies);
        movieList.setMoviesCount(count);
        return movieList;
    }

    private Map<Long, Set<String>> findMovieActors(final Collection<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<MovieActorsMapper.MovieIdActor> movieActors = actorRepository.findMovieActors(movieIds);
        return movieActors
                .stream()
                .collect(groupingBy(
                        MovieActorsMapper.MovieIdActor::getMovieId,
                        mapping(MovieActorsMapper.MovieIdActor::getActor, toSet())
                ));
    }

}
