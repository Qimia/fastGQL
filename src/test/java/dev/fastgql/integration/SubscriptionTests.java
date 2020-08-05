package dev.fastgql.integration;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public interface SubscriptionTests extends SetupTearDownForEach {

  /**
   * Test GraphQL subscription / response given in input directory.
   *
   * @param directory directory in resources
   * @param vertx vertx instance
   * @param context vertx test context
   */
  @ParameterizedTest(name = "{index} => Test: [{arguments}]")
  @MethodSource("dev.fastgql.integration.ResourcesTestUtils#subscriptionDirectories")
  default void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context) {
    System.out.println(String.format("Test: %s", directory));
    try {
      DBTestUtils.executeSQLQueryFromResource(
          Paths.get(directory, "init.sql").toString(),
          getJdbcUrlForMultipleQueries(),
          getJdbcDatabaseContainer().getUsername(),
          getJdbcDatabaseContainer().getPassword());
    } catch (SQLException | IOException e) {
      context.failNow(e);
      return;
    }

    WebClient client = WebClient.create(vertx);
    HttpClient httpClient =
        vertx.createHttpClient(new HttpClientOptions().setDefaultPort(getDeploymentPort()));
    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
        List.of(
            String.format("%s/expected-1.json", directory),
            String.format("%s/expected-2.json", directory));
    client
        .get(getDeploymentPort(), "localhost", "/update")
        .rxSend()
        .flatMap(response -> httpClient.rxWebSocket("/graphql"))
        .flatMapCompletable(
            webSocket ->
                GraphQLTestUtils.startSubscription(query, context, webSocket)
                    .andThen(GraphQLTestUtils.verifySubscription(expected, context, webSocket)))
        .subscribe(
            () ->
                DBTestUtils.executeSQLQueryFromResourceWithContext(
                    String.format("%s/query.sql", directory), getJdbcDatabaseContainer(), context),
            context::failNow);
  }
}
