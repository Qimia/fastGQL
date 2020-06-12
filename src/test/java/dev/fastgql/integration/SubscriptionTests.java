package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public interface SubscriptionTests extends SetupTearDownForEach {
  @ParameterizedTest(name = "{index} => Test: [{arguments}]")
  @MethodSource("dev.fastgql.integration.TestUtils#subscriptionDirectories")
  default void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context) {
    System.out.println(String.format("Test: %s", directory));
    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
        List.of(
            String.format("%s/expected-1.json", directory),
            String.format("%s/expected-2.json", directory));
    GraphQLTestUtils.verifySubscription(getDeploymentPort(), query, expected, vertx, context);
    DBTestUtils.executeSQLQueryFromResourceWithDelay(
        String.format("%s/query.sql", directory),
        10,
        TimeUnit.SECONDS,
        getJdbcDatabaseContainer(),
        context);
  }
}
