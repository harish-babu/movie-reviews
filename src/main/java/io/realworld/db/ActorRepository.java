package io.realworld.db;

import io.realworld.db.mapper.ArticleTagsMapper;
import io.realworld.db.mapper.MovieActorsMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ActorRepository {
    @SqlQuery("SELECT NAME FROM actors")
    List<String> findAllTags();

    @SqlQuery("SELECT distinct(NAME) FROM actors WHERE NAME IN (<names>)")
    Set<String> findActors(@BindList("names") Collection<String> names);

    @SqlBatch("INSERT INTO actors (NAME) VALUES (?)")
    void saveActors(Set<String> names);

    @SqlBatch("INSERT INTO movie_actors (MOVIE_ID, ACTOR_ID) " +
            "VALUES (?, (SELECT ID FROM actors WHERE name = ?)) " +
            "ON CONFLICT DO NOTHING")
    void addActorsToMovies(Collection<Long> movieIds, Collection<String> actors);

    @SqlUpdate("DELETE FROM movie_actors WHERE MOVIE_ID = :movieId")
    void deleteMovieActors(@Bind("movieId") long movieId);

    @SqlUpdate("DELETE FROM movie_actors WHERE MOVIE_ID in (SELECT distinct(ID) FROM movies WHERE SLUG = :slug)")
    void deleteMovieActors(@Bind("slug") String slug);

    @SqlQuery("SELECT mv.MOVIE_ID, t.NAME FROM movie_actors mv " +
            "INNER JOIN actors t ON mv.ACTOR_ID = t.ID " +
            "WHERE mv.MOVIE_ID in (<movieIds>)")
    @RegisterRowMapper(MovieActorsMapper.class)
    List<MovieActorsMapper.MovieIdActor> findMovieActors(@BindList("movieIds") Collection<Long> movieIds);

}
