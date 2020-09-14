/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import dev.fastgql.modules.*;
import io.reactivex.Single;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;

public class FastGQL extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand(
        "run", FastGQL.class.getName(), "--conf", "src/main/resources/conf-postgres.json");
  }

  protected Injector createInjector() {
    return Guice.createInjector(
        new VertxModule(vertx, config()),
        new ServerModule(),
        new GraphQLModule(),
        new DatabaseModule(),
        new SQLExecutorModule(),
        new GraphiQLModule(),
        new PermissionsSourceCodeModule());
  }

  private void startServer(Injector injector, Promise<Void> promise) {
    injector
        .getInstance(new Key<Single<HttpServer>>() {})
        .subscribe(server -> promise.complete(), promise::fail);
  }

  @Override
  public void start(Promise<Void> promise) {
    Injector injector = createInjector();
    startServer(injector, promise);
  }
}
