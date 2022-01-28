package io.realworld.db.mapper;


import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MovieActorsMapper implements RowMapper<MovieActorsMapper.MovieIdActor> {

    @Override
    public MovieActorsMapper.MovieIdActor map(final ResultSet rs, final StatementContext ctx) throws SQLException {
        final var tuple = new MovieActorsMapper.MovieIdActor();
        tuple.setMovieId(rs.getLong("MOVIE_ID"));
        tuple.setActor(rs.getString("NAME"));
        return tuple;
    }

    public static class MovieIdActor {
        private Long movieId;
        private String actor;


        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public String getActor() {
            return actor;
        }

        public void setActor(String actor) {
            this.actor = actor;
        }
    }
}
