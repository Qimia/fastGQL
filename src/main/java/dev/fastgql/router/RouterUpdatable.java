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
  private final boolean withApollo;
  private final GraphQLHandlerUpdatable graphQLHandlerUpdatable;
  private final ApolloWSHandlerUpdatable apolloWSHandlerUpdatable;

  public static RouterUpdatable create(Vertx vertx) {
    return new RouterUpdatable(vertx, false);
  }

  public static RouterUpdatable createWithApollo(Vertx vertx) {
    return new RouterUpdatable(vertx, true);
  }

  private RouterUpdatable(Vertx vertx, boolean withApollo) {
    this.withApollo = withApollo;
    graphQLHandlerUpdatable = GraphQLHandlerUpdatable.create();
    apolloWSHandlerUpdatable = ApolloWSHandlerUpdatable.create();
    router = Router.router(vertx);
    if (withApollo) {
      router.route("/graphql").handler(apolloWSHandlerUpdatable);
    }
    router.route("/graphql").handler(graphQLHandlerUpdatable);
    router
        .route("/graphiql/*")
        .handler(GraphiQLHandler.create(new GraphiQLHandlerOptions().setEnabled(true)));
  }

  public void update(GraphQL graphQL) {
    graphQLHandlerUpdatable.updateGraphQL(graphQL);
    if (withApollo) {
      apolloWSHandlerUpdatable.updateGraphQL(graphQL);
    }
  }

  public Router getRouter() {
    return router;
  }
}
