package io.realworld.security;

import java.security.Principal;

public class UserPrincipal implements Principal {
    private final String username;
    private final String profiles;

    public UserPrincipal(final String username) {
        this.username = username;
        this.profiles = new String();
    }
    public UserPrincipal(final String username, final String profiles) {
        this.username = username;
        this.profiles = profiles;
    }

    public String getUsername() {
        return username;
    }

    public String getProfiles() {
        return profiles;
    }

    @Override
    public String getName() {
        return getUsername();
    }
}
