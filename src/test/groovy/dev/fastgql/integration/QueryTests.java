package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.nio.file.Paths;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public interface QueryTests extends SetupTearDownForAll {

  /**
   * Test GraphQL query / response given in input directory.
   *
   * @param directory directory in resources
   * @param vertx vertx instance
   * @param context vertx test context
   */
  @ParameterizedTest(name = "{index} => Test: [{arguments}]")
  @MethodSource("dev.fastgql.integration.ResourcesTestUtils#queryDirectories")
  default void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context) {
    System.out.println(String.format("Test: %s", directory));
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    WebClient client = WebClient.create(vertx);
    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(rows -> client.get(getDeploymentPort(), "localhost", "/update").rxSend())
        .subscribe(
            response ->
                GraphQLTestUtils.verifyQuerySimple(directory, getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
