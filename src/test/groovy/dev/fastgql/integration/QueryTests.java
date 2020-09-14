package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.io.IOException;
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
  default void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context)
      throws IOException {
    System.out.println(String.format("Test: %s", directory));
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    String permissionsScript =
        ResourcesTestUtils.readResource(Paths.get(directory, "permissions.groovy").toString());

    WebClient client = WebClient.create(vertx);
    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
                client
                    .post(getDeploymentPort(), "localhost", "/v1/permissions")
                    .rxSendBuffer(Buffer.buffer(permissionsScript)))
        .flatMap(rows -> client.get(getDeploymentPort(), "localhost", "/v1/update").rxSend())
        .subscribe(
            response ->
                GraphQLTestUtils.verifyQuerySimple(
                    directory, getDeploymentPort(), null, vertx, context),
            context::failNow);
  }
}
