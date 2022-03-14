# ![Moview Review Example App using Java 11 and Dropwizard](logo.png)

> ### Dropwizard + JDBI3 codebase that tries to adhere to the [Movie Review](https://github.com/gothinkster/realworld-example-apps) spec and API.

This codebase was created to demonstrate a backend application built with [Dropwizard](https://www.dropwizard.io) including CRUD operations, authentication, pagination and more.

This is based on the RealWorld example.  For more information about RealWorld exampel, head over to the [RealWorld](https://github.com/gothinkster/realworld) repo.

# How it works

The application uses Dropwizard.

And the code organize as this:

1. `api` contains the data transfer objects used in the REST services API
2. `resources` contains the REST resource classes
3. `core` contains the business logic implemented as service classes
4. `db` contains the persistence layer (data access objects)
5. `security` contains all the security related functionality (JWT)

# Security

Simple JWT filter integrated in Dropwizard auth mechanism based on [JJWT](https://github.com/jwtk/jjwt) library.

The JWT secret key can be configured in `config.yml` file.

# Database

The application uses PostgreSQL database.

# Getting started

You need Java 11 or greater installed.

How to start the RealWorld application
---

1. Build the application
    ```
    mvn clean install
    ```
2. Start PostgreSQL database 
    ```
    docker-compose -f docker-compose up db
    ```
3. Migrate application database schema to latest version 
    ```
    java -jar target/moviewreview-example-app-1.0-SNAPSHOT.jar db migrate config.yml
    ```
4. Start the application 
    ```
    java -jar target/moviewreview-example-app-1.0-SNAPSHOT.jar server config.yml
    ```
5. To verify that the application is running check the following URLs 
    ```
    http://localhost:8080 
    http://localhost:8081
    ```

or use docker 

1. Build your application
    ```
    mvn clean install
    ```
2. Start containers 
    ```
    docker-compose -f docker-compose.yml up
    ```

Health Check
---

To see your applications health enter url 
```
http://localhost:8081/healthcheck
```

How to run Sonar code quality check
---

Sonar can be used to analyse code quality. You can start a local Sonar server (accessible on http://localhost:9001) with:

```
docker-compose -f sonar.yml up -d
```

Wait for Sonar to start (check http://localhost:9001), then run a Sonar analysis:

```
 mvn clean install sonar:sonar -Dsonar.host.url=http://localhost:9001
```

# Adding Transactional Capability
Using @JdbiUnitOfWork
---

Hibernate provides a handy annotation @UnitOfWork to encompass the resource level activities in to a single transaction.  Unfortunately, JDBI does not have such a capability.

[Dropwizard Unit Of Work](https://github.com/isopropylcyanide/dropwizard-jdbi-unitofwork/tree/v1.1) provides such a capability to JDBI.  It lets you annotate your resource methods in the following manner.


```
@POST
@Path("/")
@JdbiUnitOfWork
public RequestResponse createRequest() {
      ..do stateful work (across multiple Dao's)
      return response 
}
```

To use it, include the maven dependency in you pom.xml
```
<dependency>
    <groupId>com.github.isopropylcyanide</groupId>
    <artifactId>dropwizard-jdbi-unitofwork</artifactId>
    <version>1.0</version>
 </dependency>
 ```

 In the Application run() method, create a JdbiHandleManager.  RequestScopedJdbiHandleManager should be sufficient in you are not delegating the request handling to worker threads

```

@Override
public void run(final RealWorldConfiguration config, final Environment env) {
    ...

    final Jdbi jdbi = new JdbiFactory().build(env, config.getDataSourceFactory(), "database");
    final JdbiHandleManager jdbiHandleManager = new RequestScopedJdbiHandleManager(jdbi);
    
    ...
}

```

Register JdbiUnitOfWorkApplicationEventListener with Jersey environment.

```
@Override
public void run(final RealWorldConfiguration config, final Environment env) {
    ...

    // Register Application Even Listener for Unit Of Work
    env.jersey().register(new JdbiUnitOfWorkApplicationEventListener(jdbiHandleManager, new HashSet<String>()));

    ...
}
```


Create the Dao/Repositories using proxies.

```
@Override
public void run(final RealWorldConfiguration config, final Environment env) {
    ...

    final ReviewRepository reviewRepository = createNewProxy (ReviewRepository.class, jdbiHandleManager);
    final CommentRepository commentRepository = createNewProxy (CommentRepository.class, jdbiHandleManager);
    final UserRepository userRepository = createNewProxy (UserRepository.class, jdbiHandleManager);

    ...
}
```


> The original version 1.1 of the package supports only JDBI 2.0.  There is a [PR](https://github.com/isopropylcyanide/dropwizard-jdbi-unitofwork/pull/51) provided that updates the code for JDBI 3.0.  In our case, we did this PR merging manually and created an artifact, checked in to a local maven repo and added that as a dependency.  In the future, this should be provided from the maven central.
