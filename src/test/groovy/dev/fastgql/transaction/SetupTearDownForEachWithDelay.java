package dev.fastgql.transaction;

import dev.fastgql.integration.SetupTearDownForEach;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.BeforeEach;

public interface SetupTearDownForEachWithDelay extends SetupTearDownForEach {
  @BeforeEach
  default void beforeEach(Vertx vertx, VertxTestContext context) {
    setup(vertx, context, new FastGQLWithDelay());
  }
}
