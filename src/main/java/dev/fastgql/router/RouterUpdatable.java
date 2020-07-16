/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.router;

import dev.fastgql.graphql.GraphQLFactory;
import graphql.GraphQL;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import java.sql.SQLException;
import org.apache.log4j.Logger;

/**
 * Class which contains {@link Router} instance, and allows for updating this instance with new
 * {@link GraphQL} at runtime.
 *
 * @author Kamil Bobrowski
 */
public class RouterUpdatable {

  private static final Logger log = Logger.getLogger(RouterUpdatable.class);

  private final Router router;
  private final boolean withSubscription;
  private final boolean withQuery;

  private final GraphQLHandlerUpdatable graphQLHandlerUpdatable;
  private final ApolloWSHandlerUpdatable apolloWSHandlerUpdatable;

  public static RouterUpdatable createWithQueryAndSubscription(Vertx vertx, JsonObject config) {
    return new RouterUpdatable(vertx, config, true, true);
  }

  /*
    public static RouterUpdatable createWithQuery(Vertx vertx) {
      return new RouterUpdatable(vertx, true, false);
    }

    public static RouterUpdatable createWithSubscription(Vertx vertx) {
      return new RouterUpdatable(vertx, false, true);
    }
  */

  private RouterUpdatable(
      Vertx vertx, JsonObject config, boolean withQuery, boolean withSubscription) {
    log.debug("creating router, withQuery=" + withQuery + ", withSubscription=" + withSubscription);
    this.withQuery = withQuery;
    this.withSubscription = withSubscription;
    graphQLHandlerUpdatable = GraphQLHandlerUpdatable.create();
    apolloWSHandlerUpdatable = ApolloWSHandlerUpdatable.create();
    router = Router.router(vertx);
    secureRouter(vertx, config);
    if (withSubscription) {
      router.route("/graphql").handler(apolloWSHandlerUpdatable);
    }
    if (withQuery) {
      router.route("/graphql").handler(graphQLHandlerUpdatable);
    }
    router
        .route("/graphiql/*")
        .handler(GraphiQLHandler.create(new GraphiQLHandlerOptions().setEnabled(true)));
    router
        .route("/update")
        .handler(
            context -> {
              GraphQL graphQL = null;
              try {
                graphQL = GraphQLFactory.getGraphQL(config, vertx);
              } catch (SQLException e) {
                e.printStackTrace();
              }
              update(graphQL);
              HttpServerResponse response = context.response();
              response.putHeader("content-type", "text/html").end("updated");
            });
  }

  /**
   * Set a new {@link GraphQL}.
   *
   * @param graphQL new GraphQL
   */
  public void update(GraphQL graphQL) {
    if (withQuery) {
      graphQLHandlerUpdatable.updateGraphQL(graphQL);
    }
    if (withSubscription) {
      apolloWSHandlerUpdatable.updateGraphQL(graphQL);
    }
  }

  /**
   * Secure /graphql using JWT
   *
   * @param vertx the Vert.x instance
   * @param config configuration of verticle
   */
  private void secureRouter(Vertx vertx, JsonObject config) {
    if (config.containsKey("auth")) {
      JsonObject optionsJson = config.getJsonObject("auth");
      if (optionsJson.containsKey("algorithm") && optionsJson.containsKey("publicKey")) {
        JWTAuthOptions jwtAuthOptions =
            new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions(optionsJson));
        JWTAuth jwt = JWTAuth.create(vertx, jwtAuthOptions);
        router.route("/graphql").handler(JWTAuthHandler.create(jwt));
        log.debug("Succeeded in securing /graphql.");
      } else {
        log.warn("Failed to secured /graphql: algorithm or publicKey is missing.");
      }
    }
  }

  public Router getRouter() {
    return router;
  }
}
