package dev.fastgql.modules;

import dagger.Module;
import dagger.Provides;
import dev.fastgql.modules.Annotations.ServerPort;
import dev.fastgql.router.RouterUpdatable;
import io.reactivex.Single;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
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
  static Router provideRouter(Vertx vertx, JsonObject config) {
    return RouterUpdatable.createWithQueryAndSubscription(vertx, config).getRouter();
  }

  @Provides
  @Singleton
  static Single<HttpServer> provideHttpServer(
      Vertx vertx, HttpServerOptions httpServerOptions, Router router, @ServerPort int port) {
    return vertx.createHttpServer(httpServerOptions).requestHandler(router).rxListen(port);
  }
}
