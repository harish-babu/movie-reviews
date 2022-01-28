package io.realworld.db;

import com.codahale.metrics.annotation.Timed;
import io.realworld.api.response.Movie;
import io.realworld.db.mapper.MovieMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;
import java.util.Set;

@Timed
public interface MoviesRepository {

    @GetGeneratedKeys
    @SqlUpdate("INSERT INTO movies (TITLE, DESCRIPTION, BODY, YEAR_RELEASED, LANGUAGES, CREATED_AT, UPDATED_AT) " +
            "VALUES (:title, :description, :body, :yearReleased, :languages, current_timestamp, current_timestamp)")
    Long saveMovie (@Bind("title") String title,
                    @Bind("description") String description,
                    @Bind("body") String body,
                    @Bind("yearReleased") String yearReleased,
                    @Bind("languages") String languages);

    @SqlUpdate("INSERT INTO favorite_movies (USER_ID, MOVIE_ID) VALUES (:userId, :movieId)")
    void likeMovie(@Bind("userId") Long userId, @Bind("movieId") Long movieId);

    @SqlUpdate("DELETE FROM favorite_movies where USER_ID = :userId and MOVIE_ID = :movieId")
    void unLikeMovie(@Bind("userId") Long userId, @Bind("movieId") Long movieId);

    @SqlQuery("SELECT distinct(movies.ID), movies.*, users.USERNAME, users.BIO, users.IMAGE FROM movies " +
            "LEFT JOIN movie_actors ON movies.ID = movie_actors.MOVIE_ID " +
            "LEFT JOIN actors ON actors.ID = movie_actors.ACTOR_ID " +
            "LEFT JOIN favorite_movies ON movies.ID = favorite_movies.MOVIE_ID " +
            "LEFT JOIN users ON favorite_movies.USER_ID = users.ID " +
            "WHERE (:yearReleased IS NULL OR movies.YEAR_RELEASED=:yearReleased) " +
            "AND (:actor IS NULL OR actors.NAME =:actor) " +
            "AND (:favoritedBy IS NULL OR :favoritedBy = favorite_movies.USER_ID) " +
            "ORDER BY movies.CREATED_AT DESC " +
            "LIMIT :limit " +
            "OFFSET :offset")
    @RegisterRowMapper(MovieMapper.class)
    List<Movie> findMovies(@Bind("actor") String actor,
                                 @Bind("yearReleased") String yearReleased,
                                 @Bind("favoritedBy") Long favoritedBy,
                                 @Bind("offset") int offset,
                                 @Bind("limit") int limit);

    @SqlQuery("SELECT count(distinct movies.ID) FROM movies " +
            "LEFT JOIN movie_actors ON movies.ID = movie_actors.MOVIE_ID " +
            "LEFT JOIN actors ON actors.ID = movie_actors.ACTOR_ID " +
            "LEFT JOIN favorite_movies ON movies.ID = favorite_movies.MOVIE_ID " +
            "LEFT JOIN users ON favorite_movies.USER_ID = users.ID " +
            "WHERE (:yearReleased IS NULL OR movies.YEAR_RELEASED=:yearReleased) " +
            "AND (:actor IS NULL OR actors.NAME =:actor) " +
            "AND (:favoritedBy IS NULL OR :favoritedBy = favorite_movies.USER_ID)")
    int countMovies(@Bind("actor") String actor, @Bind("yearReleased") String yearReleased, @Bind("favoritedBy") Long favoritedBy);


    @SqlQuery("SELECT movies.* FROM movies " +
            "WHERE ID = :movieId")
    @RegisterRowMapper(MovieMapper.class)
    Movie findMovieById(@Bind("movieId") long movieId);

    @SqlQuery("SELECT MOVIE_ID FROM favorite_movies " +
            "INNER JOIN users on users.ID = favorite_movies.USER_ID " +
            "WHERE users.USERNAME = :username " +
            "AND MOVIE_ID in (<movieIds>)")
    Set<Long> findFavouriteMovies(@Bind("username") String username,
                                   @BindList("movieIds") Set<Long> movieIds);

    @SqlUpdate("UPDATE movies SET LIKES_COUNT = LIKES_COUNT + 1 where ID = :movieId")
    void incrementFavoritesCount(@Bind("movieId") Long movieId);

    @SqlUpdate("UPDATE movies SET LIKES_COUNT = LIKES_COUNT - 1 where ID = :movieId")
    void decrementFavoritesCount(@Bind("movieId") Long movieId);


}
