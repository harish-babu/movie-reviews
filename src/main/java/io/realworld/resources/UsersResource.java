package io.realworld.resources;

import com.codahale.metrics.annotation.Timed;
import io.dropwizard.auth.Auth;
import io.realworld.api.request.NewUser;
import io.realworld.api.request.UpdatedUser;
import io.realworld.api.response.MovieReviewList;
import io.realworld.api.response.User;
import io.realworld.core.ReviewsService;
import io.realworld.core.UserService;
import io.realworld.security.UserPrincipal;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Timed
@Path("users")
public class UsersResource {

    private final UserService userService;
    private final ReviewsService reviewsService;

    public UsersResource(final UserService userService, final ReviewsService reviewsService) {
        this.userService = userService;
        this.reviewsService = reviewsService;
    }

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@Auth final User principal) {
        return Response.ok(Map.of("user", principal)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("ADMIN")
    public Response register(@Auth final UserPrincipal principal, @Valid @NotNull final NewUser newUser) {
        final User user = userService.saveUser(newUser);

        return Response.status(Response.Status.CREATED)
                .entity(Map.of("user", user))
                .build();
    }

    @GET
    @Path("current")
    @Produces(MediaType.APPLICATION_JSON)
    public Response currentUser(@Auth final UserPrincipal principal) {
        final User user = userService.findByUsername(principal.getUsername());
        return Response.ok(Map.of("user", user)).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@Auth final UserPrincipal principal, @PathParam("id") final String id, @Valid @NotNull final UpdatedUser user) {
        final User updatedUser = userService.updateUser(id, user);
        return Response.ok(Map.of("user", updatedUser)).build();
    }

    @GET
    @Path("{id}/feed")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFeed(@Auth final UserPrincipal principal, @PathParam("id") final String id,
                            @DefaultValue("0") @QueryParam("offset") @Min(0) final int offset,
                            @DefaultValue("20") @QueryParam("limit") @Min(0) @Max(100) final int limit) {
        final MovieReviewList articles = reviewsService.findFeed(principal.getUsername(), offset, limit);

        return Response.ok(articles).build();
    }

}
