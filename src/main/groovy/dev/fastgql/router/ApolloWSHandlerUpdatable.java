/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.router;

import dev.fastgql.security.JWTConfig;
import graphql.GraphQL;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;
import io.vertx.ext.web.handler.graphql.ApolloWSOptions;
import io.vertx.reactivex.core.Vertx;
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
  private final JWTAuth jwtAuth;
  private static final Function<ApolloWSMessage, Object> DEFAULT_QUERY_CONTEXT_FACTORY = rc -> rc;
  private static final Function<ApolloWSMessage, DataLoaderRegistry>
      DEFAULT_DATA_LOADER_REGISTRY_FACTORY = rc -> null;
  private Function<ApolloWSMessage, Object> queryContextFactory = DEFAULT_QUERY_CONTEXT_FACTORY;
  private Function<ApolloWSMessage, DataLoaderRegistry> dataLoaderRegistryFactory =
      DEFAULT_DATA_LOADER_REGISTRY_FACTORY;

  private ApolloWSHandlerUpdatable(ApolloWSOptions options, JWTAuth jwtAuth) {
    this.options = options;
    this.jwtAuth = jwtAuth;
  }

  public static ApolloWSHandlerUpdatable create() {
    return new ApolloWSHandlerUpdatable(new ApolloWSOptions(), null);
  }

  public static ApolloWSHandlerUpdatable create(JWTConfig jwtConfig, Vertx vertx) {
    io.vertx.reactivex.ext.auth.jwt.JWTAuth jwtAuth = jwtConfig.getJWTAuth(vertx);
    if (jwtAuth == null) {
      return new ApolloWSHandlerUpdatable(new ApolloWSOptions(), null);
    } else {
      return new ApolloWSHandlerUpdatable(
          new ApolloWSOptions(), jwtConfig.getJWTAuth(vertx).getDelegate());
    }
  }

  /**
   * Set new GraphQL by pushing new {@link ApolloWSHandler} on internal {@link BehaviorSubject}.
   *
   * @param graphQL new GraphQL
   */
  public synchronized void updateGraphQL(GraphQL graphQL) {
    apolloWSHandlerSubject.onNext(
        ApolloWSHandler.create(graphQL, options)
            .messageHandler(
                message -> {
                  if (message.type().equals(ApolloWSMessageType.CONNECTION_INIT)) {
                    Promise<User> promise = Promise.promise();
                    message.getDelegate().setHandshake(promise.future());
                    if (jwtAuth == null) {
                      promise.complete(null);
                      return;
                    }
                    JsonObject payload = message.content().getJsonObject("payload");
                    if (payload != null && payload.containsKey("headers")) {
                      JsonObject headers = payload.getJsonObject("headers");
                      if (headers.containsKey("authorization")) {
                        String authorization = headers.getString("authorization");
                        try {
                          int idx = authorization.indexOf(' ');
                          if (idx < 0) {
                            promise.fail("wrong authorization format");
                            return;
                          }
                          if (!authorization.substring(0, idx).equals("Bearer")) {
                            promise.fail("only Bearer authorization supported");
                            return;
                          }

                          jwtAuth.authenticate(
                              new JsonObject().put("jwt", authorization.substring(idx + 1)),
                              userAsyncResult -> {
                                if (userAsyncResult.succeeded()) {
                                  promise.complete(userAsyncResult.result());
                                } else {
                                  promise.fail(userAsyncResult.cause());
                                }
                              });
                        } catch (RuntimeException e) {
                          promise.fail(e);
                        }
                      } else {
                        promise.fail("no authorization in headers");
                      }
                    } else {
                      promise.fail("no headers in payload");
                    }
                  }
                })
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
