package dev.fastgql.integration;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
  @Timeout(value = 10, timeUnit = TimeUnit.DAYS)
  default void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context)
      throws IOException {

    Pool pool = getPool(vertx);
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    System.out.println(String.format("Test: %s", directory));

    String permissionsScript =
        ResourcesTestUtils.readResource(Paths.get(directory, "permissions.groovy").toString());

    WebClient client = WebClient.create(vertx);
    HttpClient httpClient =
        vertx.createHttpClient(new HttpClientOptions().setDefaultPort(getDeploymentPort()));
    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
        List.of(
            String.format("%s/expected-1.json", directory),
            String.format("%s/expected-2.json", directory));

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
                client
                    .post(getDeploymentPort(), "localhost", "/v1/permissions")
                    .rxSendBuffer(Buffer.buffer(permissionsScript)))
        .flatMap(
            rows ->
                client
                    .get(getDeploymentPort(), "localhost", "/v1/update")
                    .rxSend()
                    .flatMap(response -> httpClient.rxWebSocket("/v1/graphql"))
                    .flatMapCompletable(
                        webSocket ->
                            GraphQLTestUtils.startSubscription(query, context, webSocket)
                                .andThen(
                                    GraphQLTestUtils.verifySubscription(
                                        expected, context, webSocket)))
                    .andThen(
                        DBTestUtils.executeSQLQuery(
                            String.format("%s/query.sql", directory), pool)))
        .subscribe(rows -> {}, context::failNow);
  }
}
