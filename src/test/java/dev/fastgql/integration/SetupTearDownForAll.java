package dev.fastgql.integration;

import dev.fastgql.FastGQL;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface SetupTearDownForAll extends WithFastGQL {
  @BeforeAll
  default void beforeAll(Vertx vertx, VertxTestContext context) {
    setup(vertx, context, new FastGQL());
  }

  @AfterAll
  default void afterAll(Vertx vertx, VertxTestContext context) {
    tearDown(vertx, context);
  }
}
