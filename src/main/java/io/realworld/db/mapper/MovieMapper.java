package io.realworld.db.mapper;

import io.realworld.api.response.Movie;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class MovieMapper implements RowMapper<Movie> {

    @Override
    public Movie map(final ResultSet rs, final StatementContext ctx) throws SQLException {
        final Movie movie = new Movie();
        movie.setId(rs.getLong("ID"));
        movie.setTitle(rs.getString("TITLE"));
        movie.setDescription(rs.getString("DESCRIPTION"));
        movie.setBody(rs.getString("BODY"));
        movie.setLanguages(rs.getString("LANGUAGES"));
        movie.setYearReleased(rs.getString("YEAR_RELEASED"));
        movie.setLikesCount(rs.getInt("LIKES_COUNT"));
        movie.setCreatedAt(toInstant(rs, "CREATED_AT"));
        movie.setUpdatedAt(toInstant(rs, "UPDATED_AT"));
        return movie;
    }

    private Instant toInstant(final ResultSet rs, final String dateColumn) throws SQLException {
        final LocalDateTime date = rs.getObject(dateColumn, LocalDateTime.class);
        return date == null ? null : date.toInstant(ZoneOffset.UTC);
    }

}
