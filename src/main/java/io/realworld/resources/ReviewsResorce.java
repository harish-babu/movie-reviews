package io.realworld.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.realworld.api.request.UpdatedArticle;
import io.realworld.api.response.MovieReview;
import io.realworld.api.response.MovieReviewList;
import io.realworld.core.ReviewsService;
import io.realworld.security.UserPrincipal;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;
import java.util.Optional;

@Timed
@Path("reviews")
public class ReviewsResorce {

    private final ReviewsService reviewsService;

    public ReviewsResorce(final ReviewsService reviewsService) {
        this.reviewsService = reviewsService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findArticles(@Auth final Optional<UserPrincipal> optionalAuthenticatedUser,
                                 @QueryParam("movieId") final Long movieId,
                                 @QueryParam("author") final String author,
                                 @QueryParam("tag") final String tag,
                                 @QueryParam("favorited") final String favoritedBy,
                                 @DefaultValue("0") @QueryParam("offset") @Min(0) final int offset,
                                 @DefaultValue("20") @QueryParam("limit") @Min(0) @Max(100) final int limit) {
        final String username = optionalAuthenticatedUser.map(UserPrincipal::getUsername).orElse(null);
        final MovieReviewList articles = reviewsService.findReviews(username, movieId, author, tag, favoritedBy, offset, limit);

        return Response.ok(articles).build();
    }

    @GET
    @Path("{slug}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findArticle(@Auth final Optional<UserPrincipal> optionalAuthenticatedUser,
                                @PathParam("slug") final String slug, @Context Request request) {
        final String username = optionalAuthenticatedUser.map(UserPrincipal::getUsername).orElse(null);
        final MovieReview movieReview = reviewsService.findBySlug(username, slug);
        EntityTag t = new EntityTag(movieReview.getUpdatedAt().toString());

        return Response.ok(Map.of("article", movieReview)).tag(t).build();
    }

    @DELETE
    @Path("{slug}")
    public Response deleteArticle(@PathParam("slug") final String slug) {
        reviewsService.deleteReview(slug);

        return Response.noContent().build();
    }

    @PUT
    @Path("{slug}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Transaction
    public Response updateArticle(@Auth final UserPrincipal principal,
                                  @HeaderParam("If-Match") @NotEmpty final String ifMatch,
                                  @PathParam("slug") final String slug,
                                  @NotNull @Valid final UpdatedArticle update, @Context Request request) {
        final MovieReview oldMovieReview = reviewsService.findBySlug(principal.getUsername(), slug);

        EntityTag t = new EntityTag(oldMovieReview.getUpdatedAt().toString());
        Optional<Response.ResponseBuilder> responseBuilder = Optional.ofNullable(request.evaluatePreconditions(t));
        if (responseBuilder.isPresent()) {
            return responseBuilder.get().entity(oldMovieReview).tag(t).build();
        }
        final MovieReview movieReview = reviewsService.updateReview(principal.getUsername(), oldMovieReview, update);
        return Response.ok(Map.of("article", movieReview)).build();
    }

    @POST
    @Path("{slug}/favorite")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addArticleToFavourites(@Auth final UserPrincipal principal,
                                           @PathParam("slug") final String slug) {
        final MovieReview movieReview = reviewsService.addArticleToFavourites(principal.getUsername(), slug);

        return Response.ok(Map.of("article", movieReview)).build();
    }

    @DELETE
    @Path("{slug}/favorite")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeArticleFromFavourites(@Auth final UserPrincipal principal,
                                                @PathParam("slug") final String slug) {
        final MovieReview movieReview = reviewsService.removeArticleFromFavourites(principal.getUsername(), slug);

        return Response.ok(Map.of("article", movieReview)).build();
    }
}
