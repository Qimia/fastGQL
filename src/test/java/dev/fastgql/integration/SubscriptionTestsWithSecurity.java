package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public interface SubscriptionTestsWithSecurity extends SetupTearDownForAll {

  String token =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9."
          + "eyJpYXQiOjE1OTQ3MTk2NDZ9."
          + "Y88rOApF1jYtB8Gs7iqE3Bp5Jgriyqr3B7bs7RKgiUzFYcAz-"
          + "KGTmrhn8e-CbM7eqNAEN7r8AxRw5jkgXxoE1A7zQ7YXK1y9xP"
          + "iMo9VqpLMSSl-u4ujFJvijIDsYfyrmTJr3bnOctmd2Lq2LlNO"
          + "QQoarVBVZkrCa5jA654l6rIKls5DiX8-Ya9gp2TFDJ-ADG2iv"
          + "36b4XykZqZeES7qGEAm8ZCvMQ9AUawbTjIa74CIBqgZbeCWb-"
          + "o884vxFOr1qnC9_U139hIeXX2p71Q_5v0Kb7ggBgCydMmPtKT"
          + "-JEkWTBcVXfzGHP-wNKHkKzkPKu2_e1O560SWGLgfB9L-9DA";

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
    client
        .get(getDeploymentPort(), "localhost", "/v1/update")
        .bearerTokenAuthentication(token)
        .rxSend()
        .subscribe(
            response -> {
              String query = String.format("%s/query.graphql", directory);
              List<String> expected =
                  List.of(
                      String.format("%s/expected-1.json", directory),
                      String.format("%s/expected-2.json", directory));
              GraphQLTestUtils.verifySubscriptionWithToken(
                  getDeploymentPort(), query, expected, token, vertx, context);
              DBTestUtils.executeSQLQueryFromResourceWithDelay(
                  String.format("%s/query.sql", directory),
                  10,
                  TimeUnit.SECONDS,
                  getJdbcDatabaseContainer(),
                  context);
            },
            context::failNow);
  }

  /**
   * Test GraphQL subscription with security / connection will be unauthorized.
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldUnauthorized(Vertx vertx, VertxTestContext context) {
    WebClient.create(vertx)
        .get(getDeploymentPort(), "localhost", "/v1/update")
        .bearerTokenAuthentication(token)
        .rxSend()
        .subscribe(
            response ->
                GraphQLTestUtils.verifyUnauthorizedSubscription(
                    getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
