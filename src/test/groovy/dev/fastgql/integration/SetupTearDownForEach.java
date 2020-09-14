package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public interface SetupTearDownForEach extends WithFastGQL {
  @BeforeEach
  default void beforeEach(Vertx vertx, VertxTestContext context) {
    setup(vertx, context, new FastGQLForTests());
  }

  @AfterEach
  default void afterEach(Vertx vertx, VertxTestContext context) {
    tearDown(vertx, context);
  }
}
