package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public interface QueryTestsWithSecurity extends QueryTests {

  @Override
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

    String token =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJpYXQiOjE1OTQ3MTk2NDZ9.Y88rOApF1jYtB8Gs7iqE3Bp5Jgriyqr3B7bs7RKgiUzFYcAz-KGTmrhn8e-CbM7eqNAEN7r8AxRw5jkgXxoE1A7zQ7YXK1y9xPiMo9VqpLMSSl-u4ujFJvijIDsYfyrmTJr3bnOctmd2Lq2LlNOQQoarVBVZkrCa5jA654l6rIKls5DiX8-Ya9gp2TFDJ-ADG2iv36b4XykZqZeES7qGEAm8ZCvMQ9AUawbTjIa74CIBqgZbeCWb-o884vxFOr1qnC9_U139hIeXX2p71Q_5v0Kb7ggBgCydMmPtKT-JEkWTBcVXfzGHP-wNKHkKzkPKu2_e1O560SWGLgfB9L-9DA";

    WebClient client = WebClient.create(vertx);
    client
        .get(getDeploymentPort(), "localhost", "/update")
        .rxSend()
        .subscribe(
            response ->
                GraphQLTestUtils.verifyQuerySimpleWithToken(
                    directory, getDeploymentPort(), token, vertx, context),
            context::failNow);
  }

  @Test
  default void shouldUnauthorized(Vertx vertx, VertxTestContext context) {
    WebClient.create(vertx)
        .get(getDeploymentPort(), "localhost", "/update")
        .rxSend()
        .subscribe(
            response -> GraphQLTestUtils.verifyUnauthorized(getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
