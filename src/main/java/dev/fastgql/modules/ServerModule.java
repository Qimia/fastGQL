package dev.fastgql.modules;

import dagger.Module;
import dagger.Provides;
import dev.fastgql.modules.Annotations.ServerPort;
import dev.fastgql.modules.Annotations.UpdateHandler;
import dev.fastgql.router.ApolloWSHandlerUpdatable;
import dev.fastgql.router.GraphQLHandlerUpdatable;
import graphql.GraphQL;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import java.util.function.Supplier;
import javax.inject.Singleton;

@Module
public abstract class ServerModule {

  @Provides
  @Singleton
  static HttpServerOptions provideHttpServerOptions() {
    return new HttpServerOptions().setWebsocketSubProtocols("graphql-ws");
  }

  @Provides
  @Singleton
  static GraphiQLHandler provideGraphiQLHandler() {
    return GraphiQLHandler.create(new GraphiQLHandlerOptions().setEnabled(true));
  }

  @Provides
  @Singleton
  @UpdateHandler
  static Handler<RoutingContext> provideUpdateHandler(
      GraphQLHandlerUpdatable graphQLHandlerUpdatable,
      ApolloWSHandlerUpdatable apolloWSHandlerUpdatable,
      Supplier<GraphQL> graphQLSupplier) {
    return context -> {
      GraphQL graphQL = graphQLSupplier.get();
      if (graphQLHandlerUpdatable != null) {
        graphQLHandlerUpdatable.updateGraphQL(graphQL);
      }
      if (apolloWSHandlerUpdatable != null) {
        apolloWSHandlerUpdatable.updateGraphQL(graphQL);
      }
      HttpServerResponse response = context.response();
      response.putHeader("content-type", "text/html").end("updated");
    };
  }

  @Provides
  @Singleton
  static Router provideRouter(
      Vertx vertx,
      GraphQLHandlerUpdatable graphQLHandlerUpdatable,
      ApolloWSHandlerUpdatable apolloWSHandlerUpdatable,
      GraphiQLHandler graphiQLHandler,
      @UpdateHandler Handler<RoutingContext> updateHandler,
      Supplier<GraphQL> graphQLSupplier) {
    Router router = Router.router(vertx);
    GraphQL graphQL = graphQLSupplier.get();
    if (apolloWSHandlerUpdatable != null) {
      apolloWSHandlerUpdatable.updateGraphQL(graphQL);
      router.route("/graphql").handler(apolloWSHandlerUpdatable);
    }
    if (graphQLHandlerUpdatable != null) {
      graphQLHandlerUpdatable.updateGraphQL(graphQL);
      router.route("/graphql").handler(graphQLHandlerUpdatable);
    }
    if (graphiQLHandler != null) {
      router.route("/graphiql/*").handler(graphiQLHandler);
    }
    if (updateHandler != null) {
      router.route("/update").handler(updateHandler);
    }
    return router;
  }

  @Provides
  @Singleton
  static Single<HttpServer> provideHttpServer(
      Vertx vertx, HttpServerOptions httpServerOptions, Router router, @ServerPort int port) {
    return vertx.createHttpServer(httpServerOptions).requestHandler(router).rxListen(port);
  }
}
