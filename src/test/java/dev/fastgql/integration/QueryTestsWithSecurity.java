package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public interface QueryTestsWithSecurity extends SetupTearDownForAll {

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
   * Test GraphQL query with security / response given in input directory
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldAuthorized(Vertx vertx, VertxTestContext context) {
    String directory = "queries/simple/list";
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
            response ->
                GraphQLTestUtils.verifyQuerySimpleWithToken(
                    directory, getDeploymentPort(), token, vertx, context),
            context::failNow);
  }

  /**
   * Test GraphQL query with security / connection to /v1/update will be unauthorized.
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldUpdateUnauthorized(Vertx vertx, VertxTestContext context) {
    WebClient.create(vertx)
        .get(getDeploymentPort(), "localhost", "/v1/update")
        .rxSend()
        .subscribe(
            response ->
                context.verify(
                    () -> {
                      Assertions.assertEquals(401, response.statusCode());
                      context.completeNow();
                    }),
            context::failNow);
    GraphQLTestUtils.verifyUnauthorizedQuery(getDeploymentPort(), vertx, context);
  }

  /**
   * Test GraphQL query with security / connection to /v1/graphql will be unauthorized.
   *
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  @Test
  default void shouldGraphqlUnauthorized(Vertx vertx, VertxTestContext context) {
    WebClient.create(vertx)
        .get(getDeploymentPort(), "localhost", "/v1/update")
        .bearerTokenAuthentication(token)
        .rxSend()
        .subscribe(
            response ->
                GraphQLTestUtils.verifyUnauthorizedQuery(getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
