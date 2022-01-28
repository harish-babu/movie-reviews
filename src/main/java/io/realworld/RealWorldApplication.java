package io.realworld;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.Application;
import io.dropwizard.auth.PolymorphicAuthDynamicFeature;
import io.dropwizard.auth.PolymorphicAuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.realworld.api.response.User;
import io.realworld.core.*;
import io.realworld.db.*;
import io.realworld.resources.*;
import io.realworld.resources.exceptionhandling.ApplicationExceptionMapper;
import io.realworld.resources.exceptionhandling.GeneralExceptionMapper;
import io.realworld.security.*;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.jdbi.v3.core.Jdbi;

import static io.realworld.security.JwtAuthFilter.TOKEN_PREFIX;

public class RealWorldApplication extends Application<RealWorldConfiguration> {

    public static void main(final String[] args) throws Exception {
        new RealWorldApplication().run(args);
    }

    @Override
    public String getName() {
        return "RealWorld";
    }

    @Override
    public void initialize(final Bootstrap<RealWorldConfiguration> bootstrap) {
        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(final RealWorldConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        enableConfigSubstitutionWithEnvVariables(bootstrap);
    }

    @Override
    public void run(final RealWorldConfiguration config, final Environment env) {
        final Jdbi jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "database");

        final PasswordEncoder passwordEncoder = new PasswordEncoder();
        final JwtTokenService jwtTokenService = new JwtTokenService(config.getJwt());

        final ReviewRepository reviewRepository = jdbi.onDemand(ReviewRepository.class);
        final CommentRepository commentRepository = jdbi.onDemand(CommentRepository.class);
        final UserRepository userRepository = jdbi.onDemand(UserRepository.class);
        final TagRepository tagRepository = jdbi.onDemand(TagRepository.class);
        final MoviesRepository moviesRepository = jdbi.onDemand(MoviesRepository.class);
        final ActorRepository actorRepository = jdbi.onDemand(ActorRepository.class);

        final ReviewsService reviewsService = new ReviewsService(reviewRepository, userRepository, commentRepository, tagRepository);
        final MoviesService moviesService = new MoviesService(userRepository, moviesRepository, actorRepository);
        final CommentService commentService = new CommentService(commentRepository, reviewRepository, userRepository);
        final ProfileService profileService = new ProfileService(userRepository);
        final UserService userService = new UserService(userRepository, passwordEncoder, jwtTokenService);

        env.jersey().register(new ReviewsResorce(reviewsService));
        env.jersey().register(new CommentResource(commentService));
        env.jersey().register(new ProfileResource(profileService));
        env.jersey().register(new MoviesResource(userService, moviesService, reviewsService));
        env.jersey().register(new UsersResource(userService, reviewsService));
        env.jersey().register(new TagsResource(tagRepository));

        env.jersey().register(new ApplicationExceptionMapper());
        env.jersey().register(new GeneralExceptionMapper());

        configureJsonMapper(env.getObjectMapper());
        configureAuth(env.jersey(), jwtTokenService, userService);
    }

    private void configureAuth (final JerseyEnvironment env, final JwtTokenService jwtTokenService, UserService userSvc) {

        // Use Polymorphic Auth

        final JwtAuthFilter<UserPrincipal> jwtAuthFilter = new JwtAuthFilter.Builder<UserPrincipal>()
                .setPrefix(TOKEN_PREFIX)
                .setAuthenticator(jwtTokenService)
                .setAuthorizer(new RBACAuthoriser())
                .buildAuthFilter();
        final BasicCredentialAuthFilter<User> basicCredentialAuthFilter = new BasicCredentialAuthFilter.Builder<User>()
                .setAuthenticator(new PasswordAuthenticator(userSvc))
                .buildAuthFilter();

        final PolymorphicAuthDynamicFeature feature = new PolymorphicAuthDynamicFeature<>(
                ImmutableMap.of(
                        User.class, basicCredentialAuthFilter,
                        UserPrincipal.class, jwtAuthFilter));
        final AbstractBinder binder = new PolymorphicAuthValueFactoryProvider.Binder<>(
                ImmutableSet.of(UserPrincipal.class, User.class));

        env.register(RolesAllowedDynamicFeature.class);
        env.register(binder);
        env.register(feature);
    }

    private void configureJsonMapper(final ObjectMapper mapper) {
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(DeserializationFeature.UNWRAP_ROOT_VALUE);
        mapper.registerModule(new JavaTimeModule());
    }

    private void enableConfigSubstitutionWithEnvVariables(final Bootstrap<RealWorldConfiguration> bootstrap) {
        final var envVarSubst = new EnvironmentVariableSubstitutor(true);
        final var provider = new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), envVarSubst);
        bootstrap.setConfigurationSourceProvider(provider);
    }
}
