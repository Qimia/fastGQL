package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.testcontainers.containers.JdbcDatabaseContainer;

public interface AbstractContainerEnv {
  void setup(Vertx vertx, VertxTestContext context);

  void tearDown(Vertx vertx, VertxTestContext context);

  int getDeploymentPort();

  JdbcDatabaseContainer<?> getJdbcDatabaseContainer();
}
