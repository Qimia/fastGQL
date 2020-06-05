/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.router;

import graphql.GraphQL;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.graphql.GraphQLHandlerOptions;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import java.util.function.Function;
import org.dataloader.DataLoaderRegistry;

/**
 * Defines a version of {@link GraphQLHandler} which can be altered at runtime by updating it with
 * new {@link GraphQL}.
 *
 * @author Kamil Bobrowski
 */
public class GraphQLHandlerUpdatable implements Handler<RoutingContext> {
  private final Subject<GraphQLHandler> graphQLHandlerSubject =
      BehaviorSubject.<GraphQLHandler>create().toSerialized();
  private final GraphQLHandlerOptions options;
  private static final Function<RoutingContext, Object> DEFAULT_QUERY_CONTEXT_FACTORY = rc -> rc;
  private static final Function<RoutingContext, DataLoaderRegistry>
      DEFAULT_DATA_LOADER_REGISTRY_FACTORY = rc -> null;
  private Function<RoutingContext, Object> queryContextFactory = DEFAULT_QUERY_CONTEXT_FACTORY;
  private Function<RoutingContext, DataLoaderRegistry> dataLoaderRegistryFactory =
      DEFAULT_DATA_LOADER_REGISTRY_FACTORY;

  private GraphQLHandlerUpdatable(GraphQLHandlerOptions options) {
    this.options = options;
  }

  public static GraphQLHandlerUpdatable create() {
    return new GraphQLHandlerUpdatable(new GraphQLHandlerOptions());
  }

  public static GraphQLHandlerUpdatable create(GraphQLHandlerOptions options) {
    return new GraphQLHandlerUpdatable(options);
  }

  /**
   * Set new GraphQL by pushing new {@link GraphQLHandler} on internal {@link BehaviorSubject}.
   *
   * @param graphQL new GraphQL
   */
  public synchronized void updateGraphQL(GraphQL graphQL) {
    graphQLHandlerSubject.onNext(
        GraphQLHandler.create(graphQL, options)
            .queryContext(queryContextFactory)
            .dataLoaderRegistry(dataLoaderRegistryFactory));
  }

  public synchronized GraphQLHandlerUpdatable queryContext(
      Function<RoutingContext, Object> factory) {
    queryContextFactory = factory != null ? factory : DEFAULT_QUERY_CONTEXT_FACTORY;
    return this;
  }

  public synchronized GraphQLHandlerUpdatable dataLoaderRegistry(
      Function<RoutingContext, DataLoaderRegistry> factory) {
    dataLoaderRegistryFactory = factory != null ? factory : DEFAULT_DATA_LOADER_REGISTRY_FACTORY;
    return this;
  }

  @Override
  public void handle(RoutingContext ctx) {
    graphQLHandlerSubject
        .take(1)
        .doOnNext(graphQLHandler -> graphQLHandler.handle(ctx))
        .subscribe();
  }
}
