package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public interface QueryTestsWithSecurity extends SetupTearDownForAll {

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
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    WebClient client = WebClient.create(vertx);
    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
                client
                    .get(getDeploymentPort(), "localhost", "/v1/update")
                    .bearerTokenAuthentication(getJwtToken())
                    .rxSend())
        .subscribe(
            response ->
                GraphQLTestUtils.verifyQuerySimple(
                    directory, getDeploymentPort(), getJwtToken(), vertx, context),
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
    GraphQLTestUtils.verifyUnauthorizedRequest(getDeploymentPort(), "/v1/update", vertx, context);
  }

  @Test
  default void shouldGraphQLUnauthorized(Vertx vertx, VertxTestContext context) {
    GraphQLTestUtils.verifyUnauthorizedRequest(getDeploymentPort(), "/v1/graphql", vertx, context);
  }
}
