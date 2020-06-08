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
import io.vertx.ext.web.handler.graphql.ApolloWSOptions;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSMessage;
import java.util.function.Function;
import org.dataloader.DataLoaderRegistry;

/**
 * Defines a version of {@link ApolloWSHandler} which can be altered at runtime by updating it with
 * new {@link GraphQL}.
 *
 * @author Kamil Bobrowski
 */
public class ApolloWSHandlerUpdatable implements Handler<RoutingContext> {
  private final Subject<ApolloWSHandler> apolloWSHandlerSubject =
      BehaviorSubject.<ApolloWSHandler>create().toSerialized();
  private final ApolloWSOptions options;
  private static final Function<ApolloWSMessage, Object> DEFAULT_QUERY_CONTEXT_FACTORY = rc -> rc;
  private static final Function<ApolloWSMessage, DataLoaderRegistry>
      DEFAULT_DATA_LOADER_REGISTRY_FACTORY = rc -> null;
  private Function<ApolloWSMessage, Object> queryContextFactory = DEFAULT_QUERY_CONTEXT_FACTORY;
  private Function<ApolloWSMessage, DataLoaderRegistry> dataLoaderRegistryFactory =
      DEFAULT_DATA_LOADER_REGISTRY_FACTORY;

  private ApolloWSHandlerUpdatable(ApolloWSOptions options) {
    this.options = options;
  }

  public static ApolloWSHandlerUpdatable create() {
    return new ApolloWSHandlerUpdatable(new ApolloWSOptions());
  }

  public static ApolloWSHandlerUpdatable create(ApolloWSOptions options) {
    return new ApolloWSHandlerUpdatable(options);
  }

  /**
   * Set new GraphQL by pushing new {@link ApolloWSHandler} on internal {@link BehaviorSubject}.
   *
   * @param graphQL new GraphQL
   */
  public synchronized void updateGraphQL(GraphQL graphQL) {
    apolloWSHandlerSubject.onNext(
        ApolloWSHandler.create(graphQL, options)
            .queryContext(queryContextFactory)
            .dataLoaderRegistry(dataLoaderRegistryFactory));
  }

  public synchronized ApolloWSHandlerUpdatable queryContext(
      Function<ApolloWSMessage, Object> factory) {
    queryContextFactory = factory != null ? factory : DEFAULT_QUERY_CONTEXT_FACTORY;
    return this;
  }

  public synchronized ApolloWSHandlerUpdatable dataLoaderRegistry(
      Function<ApolloWSMessage, DataLoaderRegistry> factory) {
    dataLoaderRegistryFactory = factory != null ? factory : DEFAULT_DATA_LOADER_REGISTRY_FACTORY;
    return this;
  }

  @Override
  public void handle(RoutingContext ctx) {
    apolloWSHandlerSubject
        .take(1)
        .doOnNext(apolloWSHandler -> apolloWSHandler.handle(ctx))
        .subscribe();
  }
}
