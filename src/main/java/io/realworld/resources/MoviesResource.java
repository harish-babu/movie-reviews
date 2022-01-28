package io.realworld.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.dropwizard.jersey.caching.CacheControl;
import io.realworld.api.request.NewMovie;
import io.realworld.api.request.NewMovieReview;
import io.realworld.api.response.Movie;
import io.realworld.api.response.MovieList;
import io.realworld.api.response.MovieReview;
import io.realworld.api.response.MovieReviewList;
import io.realworld.core.MoviesService;
import io.realworld.core.ReviewsService;
import io.realworld.core.UserService;
import io.realworld.security.UserPrincipal;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Timed
@Path("movies")
public class MoviesResource {

    private final UserService userService;
    private final MoviesService moviesService;
    private final ReviewsService reviewsService;

    public MoviesResource(final UserService userService, final MoviesService moviesService, ReviewsService reviewsService) {
        this.userService = userService;
        this.moviesService = moviesService;
        this.reviewsService = reviewsService;
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMovie(@Auth final UserPrincipal principal,
                                  @NotNull @Valid final NewMovie newMovie) {
        final Movie movie = moviesService.createMovie(principal.getUsername(), newMovie);

        return Response.ok(Map.of("movie", movie)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CacheControl(maxAge = 5, maxAgeUnit = TimeUnit.MINUTES)
    public Response findMovies(@Auth final Optional<UserPrincipal> optionalAuthenticatedUser,
                               @QueryParam("actor") final String actor,
                               @QueryParam("yearReleased") final String yearReleased,
                               @QueryParam("favorited") final String favoritedBy,
                               @DefaultValue("0") @QueryParam("offset") @Min(0) final int offset,
                               @DefaultValue("20") @QueryParam("limit") @Min(0) @Max(100) final int limit) {
        final String username = optionalAuthenticatedUser.map(UserPrincipal::getUsername).orElse(null);
        final MovieList movies = moviesService.findMovies(username, actor, yearReleased, favoritedBy, offset, limit);

        return Response.ok(movies).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findArticle(@Auth final Optional<UserPrincipal> optionalAuthenticatedUser,
                                @PathParam("id") final String id) {
        final String username = optionalAuthenticatedUser.map(UserPrincipal::getUsername).orElse(null);
        final Movie movie = moviesService.findById(username, id);

        return Response.ok(Map.of("movie", movie)).build();
    }

    @POST
    @Path("{id}/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createArticle(@Auth final UserPrincipal principal,
                                  @PathParam("id") final String id,
                                  @NotNull @Valid final NewMovieReview newMovieReview) {
        final MovieReview movieReview = reviewsService.createReview(principal.getUsername(), id, newMovieReview);

        return Response.ok(Map.of("article", movieReview)).build();
    }

    @GET
    @Path("{id}/reviews")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response findArticles(@Auth final Optional<UserPrincipal> optionalAuthenticatedUser,
                                 @PathParam("id") final Long movieId,
                                 @QueryParam("author") final String author,
                                 @QueryParam("tag") final String tag,
                                 @QueryParam("favorited") final String favoritedBy,
                                 @DefaultValue("0") @QueryParam("offset") @Min(0) final int offset,
                                 @DefaultValue("20") @QueryParam("limit") @Min(0) @Max(100) final int limit) {
        final String username = optionalAuthenticatedUser.map(UserPrincipal::getUsername).orElse(null);
        final MovieReviewList articles = reviewsService.findReviews(username, movieId, author, tag, favoritedBy, offset, limit);

        return Response.ok(articles).build();
    }

    @POST
    @Path("{id}/like")
    @Produces(MediaType.APPLICATION_JSON)
    public Response likeMovie(@Auth final UserPrincipal principal,
                                           @PathParam("id") final String id) {
        final Movie movie = moviesService.starMovie(principal.getUsername(), id);

        return Response.ok(Map.of("article", movie)).build();
    }


    @DELETE
    @Path("{id}/like")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeArticleFromFavourites(@Auth final UserPrincipal principal,
                                                @PathParam("id") final String id) {
        final Movie movie = moviesService.unStarMovie(principal.getUsername(), id);

        return Response.ok(Map.of("article", movie)).build();
    }

}
