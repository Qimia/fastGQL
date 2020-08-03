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
import org.apache.log4j.Logger;

public class FastGQL extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(FastGQL.class);

  public static void main(String[] args) {
    Launcher.executeCommand(
        "run", FastGQL.class.getName(), "--conf", "src/main/resources/conf.json");
  }

  @Override
  public void start(Promise<Void> future) {
    Injector injector =
        Guice.createInjector(
            new VertxModule(vertx, config()),
            new ServerModule(),
            new GraphQLModule(),
            new DatabaseModule());

    injector
        .getInstance(new Key<Single<HttpServer>>() {})
        .subscribe(
            server -> {
              log.debug("deployed server");
              future.complete();
            },
            future::fail);
  }
}
