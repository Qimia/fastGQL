package dev.fastgql.integration;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public interface SubscriptionTestsWithSecurity extends SetupTearDownForAll {

  /**
   * Test GraphQL subscription with security / response given in input directory.
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldAuthorized(Vertx vertx, VertxTestContext context) {
    String directory = "subscriptions/simple/list";
    System.out.println(String.format("Test: %s", directory));

    Pool pool = getPool(vertx);
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    WebClient client = WebClient.create(vertx);
    HttpClient httpClient = vertx.createHttpClient();
    WebSocketConnectOptions wsOptions =
        new WebSocketConnectOptions()
            .addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + getJwtToken())
            .setHost("localhost")
            .setPort(getDeploymentPort())
            .setURI("/v1/graphql");

    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
        List.of(
            String.format("%s/expected-1.json", directory),
            String.format("%s/expected-2.json", directory));

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
                client
                    .get(getDeploymentPort(), "localhost", "/v1/update")
                    .bearerTokenAuthentication(getJwtToken())
                    .rxSend()
                    .flatMap(response -> httpClient.rxWebSocket(wsOptions))
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

  /**
   * Test GraphQL subscription with security / connection will be unauthorized.
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldUnauthorized(Vertx vertx, VertxTestContext context) {
    WebSocketConnectOptions wsOptions =
        new WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(getDeploymentPort())
            .setURI("/v1/graphql");
    vertx
        .createHttpClient()
        .webSocket(
            wsOptions,
            websocketRes -> {
              Assertions.assertTrue(websocketRes.failed());
              context.completeNow();
            });
  }
}
