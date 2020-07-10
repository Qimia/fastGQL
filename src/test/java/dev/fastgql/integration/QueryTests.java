package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
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
        .get(getDeploymentPort(), "localhost", "/update")
        .rxSend()
        .subscribe(
            response ->
                GraphQLTestUtils.verifyQuerySimple(directory, getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
