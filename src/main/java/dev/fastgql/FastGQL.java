/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import dev.fastgql.modules.VertxModule;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import org.apache.log4j.Logger;

public class FastGQL extends AbstractVerticle {

  private static final Logger log = Logger.getLogger(FastGQL.class);

  public static void main(String[] args) {
    Launcher.executeCommand(
        "run", FastGQL.class.getName(), "--conf", "src/main/resources/conf.json");
  }

  @Override
  public void start(Promise<Void> future) {

    DaggerServerComponent.factory()
        .create(new VertxModule(vertx, config()))
        .getHttpServer()
        .subscribe(
            server -> {
              log.debug("deployed server");
              future.complete();
            },
            server -> future.fail(server.getCause()));
  }
}
