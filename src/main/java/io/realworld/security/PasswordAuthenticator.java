package io.realworld.security;


import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;
import io.realworld.api.response.User;
import io.realworld.core.UserService;
import io.realworld.exceptions.ApplicationException;

import java.util.Optional;

public class PasswordAuthenticator implements Authenticator<BasicCredentials, User> {

    private UserService userService;
    public PasswordAuthenticator (UserService userService) {
        this.userService = userService;
    }

    @Override
    public Optional<User> authenticate(BasicCredentials credentials) throws AuthenticationException {
        try {
            final User user = userService.login(credentials.getUsername(), credentials.getPassword());
            return Optional.of(new User(credentials.getUsername()));
        }
        catch (ApplicationException ae) {
            return Optional.empty();
        }
    }
}
