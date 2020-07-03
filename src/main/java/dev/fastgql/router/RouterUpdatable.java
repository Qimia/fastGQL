/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.router;

import dev.fastgql.events.DebeziumEngineSingleton;
import dev.fastgql.graphql.GraphQLFactory;
import graphql.GraphQL;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import org.apache.log4j.Logger;

import java.sql.SQLException;

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

  public Router getRouter() {
    return router;
  }
}
