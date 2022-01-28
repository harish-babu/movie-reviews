package io.realworld.db.mapper;

import io.realworld.api.response.MovieReview;
import io.realworld.api.response.Profile;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ReviewMapper implements RowMapper<MovieReview> {

    @Override
    public MovieReview map(final ResultSet rs, final StatementContext ctx) throws SQLException {
        final MovieReview movieReview = new MovieReview();
        movieReview.setId(rs.getLong("ID"));
        movieReview.setSlug(rs.getString("SLUG"));
        movieReview.setTitle(rs.getString("TITLE"));
        movieReview.setDescription(rs.getString("DESCRIPTION"));
        movieReview.setBody(rs.getString("BODY"));
        movieReview.setMovieId((rs.getLong("MOVIE_ID")));
        movieReview.setFavoritesCount(rs.getInt("FAVORITES_COUNT"));
        movieReview.setCreatedAt(toInstant(rs, "CREATED_AT"));
        movieReview.setUpdatedAt(toInstant(rs, "UPDATED_AT"));
        final Profile profile = mapProfile(rs);
        movieReview.setAuthor(profile);
        return movieReview;
    }

    private Profile mapProfile(final ResultSet rs) throws SQLException {
        final Profile profile = new Profile();
        profile.setUsername(rs.getString("USERNAME"));
        profile.setBio(rs.getString("BIO"));
        profile.setImage(rs.getString("IMAGE"));
        return profile;
    }

    private Instant toInstant(final ResultSet rs, final String dateColumn) throws SQLException {
        final LocalDateTime date = rs.getObject(dateColumn, LocalDateTime.class);
        return date == null ? null : date.toInstant(ZoneOffset.UTC);
    }
}

