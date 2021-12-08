package io.realworld.security;

import io.dropwizard.auth.Authorizer;
import io.realworld.api.response.User;

public class RBACAuthoriser implements Authorizer<UserPrincipal> {
    @Override
    public boolean authorize(UserPrincipal user, String role) {
        return user.getProfiles().contains(role);
    }
}