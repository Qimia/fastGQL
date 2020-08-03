package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.router.ApolloWSHandlerUpdatable;
import dev.fastgql.router.GraphQLHandlerUpdatable;
import graphql.GraphQL;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import dev.fastgql.modules.Annotations.ServerPort;
import dev.fastgql.modules.Annotations.UpdateHandler;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

import javax.inject.Singleton;
import java.util.function.Supplier;

public class ServerModule extends AbstractModule {

  @Provides
  @Singleton
  HttpServerOptions provideHttpServerOptions() {
    return new HttpServerOptions().setWebsocketSubProtocols("graphql-ws");
  }

  @Provides
  @Singleton
  GraphQLHandlerUpdatable provideGraphQLHandlerUpdatable() {
    return GraphQLHandlerUpdatable.create();
  }

  @Provides
  @Singleton
  ApolloWSHandlerUpdatable provideApolloWSHandlerUpdatable() {
    return ApolloWSHandlerUpdatable.create();
  }

  @Provides
  @Singleton
  GraphiQLHandler provideGraphiQLHandler() {
    return GraphiQLHandler.create(new GraphiQLHandlerOptions().setEnabled(true));
  }

  @Provides
  @Singleton
  @UpdateHandler
  Handler<RoutingContext> provideUpdateHandler(GraphQLHandlerUpdatable graphQLHandlerUpdatable, ApolloWSHandlerUpdatable apolloWSHandlerUpdatable, Supplier<GraphQL> graphQLSupplier) {
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
  @ServerPort
  int port(JsonObject config) {
    return config.getInteger("http.port", 8080);
  }

  @Provides
  Router provideRouter(Vertx vertx, GraphQLHandlerUpdatable graphQLHandlerUpdatable, ApolloWSHandlerUpdatable apolloWSHandlerUpdatable, GraphiQLHandler graphiQLHandler, @UpdateHandler Handler<RoutingContext> updateHandler) {
    Router router = Router.router(vertx);
    if (apolloWSHandlerUpdatable != null) {
      router.route("/graphql").handler(apolloWSHandlerUpdatable);
    }
    if (graphQLHandlerUpdatable != null) {
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
  Single<HttpServer> provideHttpServer(Vertx vertx, HttpServerOptions httpServerOptions, Router router, @ServerPort int port) {
    return vertx
      .createHttpServer(httpServerOptions)
      .requestHandler(router)
      .rxListen(port);
  }
}
