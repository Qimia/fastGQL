/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.router;

import graphql.GraphQL;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

public class RouterUpdatable {
  private final Router router;
  private final boolean withSubscription;
  private final boolean withQuery;

  private final GraphQLHandlerUpdatable graphQLHandlerUpdatable;
  private final ApolloWSHandlerUpdatable apolloWSHandlerUpdatable;

  public static RouterUpdatable createWithQueryAndSubscription(Vertx vertx) {
    return new RouterUpdatable(vertx, true, true);
  }

  public static RouterUpdatable createWithQuery(Vertx vertx) {
    return new RouterUpdatable(vertx, true, false);
  }

  public static RouterUpdatable createWithSubscription(Vertx vertx) {
    return new RouterUpdatable(vertx, false, true);
  }

  private RouterUpdatable(Vertx vertx, boolean withQuery, boolean withSubscription) {
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
  }

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
