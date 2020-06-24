/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import dev.fastgql.router.RouterUpdatable;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.AbstractVerticle;

public class FastGQL extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand(
        "run", FastGQL.class.getName(), "--conf", "src/main/resources/conf.json");
  }

  @Override
  public void start(Promise<Void> future) {

    RouterUpdatable routerUpdatable =
        RouterUpdatable.createWithQueryAndSubscription(vertx, config());

    vertx
        .createHttpServer(new HttpServerOptions().setWebsocketSubProtocols("graphql-ws"))
        .requestHandler(routerUpdatable.getRouter())
        .rxListen(config().getInteger("http.port", 8080))
        .doOnSuccess(server -> future.complete())
        .doOnError(server -> future.fail(server.getCause()))
        .subscribe();
  }
}
