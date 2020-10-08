package dev.fastgql.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public interface SubscriptionTestsWithSecurity extends SetupTearDownForAll, WithSecurity {

  /**
   * Test GraphQL subscription with security / response given in input directory.
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldAuthorized(Vertx vertx, VertxTestContext context) throws IOException {
    String directory = "subscriptions/simple/list";
    System.out.println(String.format("Test: %s", directory));

    Pool pool = getPool(vertx);
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    String jwtToken = getJwtToken(vertx, Map.of());

    WebClient client = WebClient.create(vertx);
    HttpClient httpClient = vertx.createHttpClient();
    WebSocketConnectOptions wsOptions =
        new WebSocketConnectOptions()
            .addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + jwtToken)
            .setHost("localhost")
            .setPort(getDeploymentPort())
            .setURI("/v1/graphql");

    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
        List.of(
            String.format("%s/expected-1.json", directory),
            String.format("%s/expected-2.json", directory));

    String permissionsScript =
        ResourcesTestUtils.readResource(Paths.get(directory, "permissions.groovy").toString());

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
                client
                    .post(getDeploymentPort(), "localhost", "/v1/permissions")
                    .bearerTokenAuthentication(jwtToken)
                    .rxSendBuffer(Buffer.buffer(permissionsScript)))
        .flatMap(
            rows ->
                client
                    .get(getDeploymentPort(), "localhost", "/v1/update")
                    .bearerTokenAuthentication(jwtToken)
                    .rxSend()
                    .flatMap(response -> httpClient.rxWebSocket(wsOptions))
                    .flatMapCompletable(
                        webSocket ->
                            GraphQLTestUtils.startSubscription(
                                    new JsonObject()
                                        .put(
                                            "headers",
                                            new JsonObject()
                                                .put("authorization", "Bearer " + jwtToken)),
                                    query,
                                    context,
                                    webSocket)
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
    String directory = "subscriptions/simple/list";
    String query = String.format("%s/query.graphql", directory);
    WebSocketConnectOptions wsOptions =
        new WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(getDeploymentPort())
            .setURI("/v1/graphql");
    vertx
        .createHttpClient()
        .rxWebSocket(wsOptions)
        .flatMapCompletable(
            webSocket ->
                GraphQLTestUtils.startSubscription(
                    new JsonObject()
                        .put("headers", new JsonObject().put("authorization", "Bearer dummy")),
                    query,
                    context,
                    webSocket))
        .subscribe(
            () -> context.failNow(new RuntimeException("subscribed")),
            error ->
                context.verify(
                    () -> {
                      assertEquals("connection error", error.getMessage());
                      context.completeNow();
                    }));
  }
}
