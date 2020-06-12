package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public interface QueryTests extends SetupTearDownForAll {
  @ParameterizedTest(name = "{index} => Test: [{arguments}]")
  @MethodSource("dev.fastgql.integration.TestUtils#queryDirectories")
  default void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context) {
    System.out.println(String.format("Test: %s", directory));
    GraphQLTestUtils.verifyQuerySimple(directory, getDeploymentPort(), vertx, context);
  }
}
