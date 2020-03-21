package ai.qimia.vertxtest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Stream;

@ExtendWith(VertxExtension.class)
public class GraphQLServerTest {

  private static final int port = 8081;

  private static Network network = Network.newNetwork();
  private static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("debezium/postgres:11")
    .withNetwork(network)
    .withNetworkAliases("postgres");


  @BeforeAll
  public static void setUp(Vertx vertx, VertxTestContext context) {

    Startables.deepStart(Stream.of(postgresContainer)).join();

    try {
      DBUtils.initializeDB(
        "init.sql",
        postgresContainer.getJdbcUrl(),
        postgresContainer.getUsername(),
        postgresContainer.getPassword()
      );
    } catch (SQLException | IOException e) {
      context.failNow(e);
      return;
    }

    JsonObject config = new JsonObject()
      .put("http.port", port)
      .put("datasource", JsonObject.mapFrom(
        DBUtils.datasourceConfig(
          postgresContainer.getJdbcUrl(),
          postgresContainer.getDatabaseName(),
          postgresContainer.getUsername(),
          postgresContainer.getPassword()
        )
      ));

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(config);
    vertx.deployVerticle(new GraphQLServer(), options, context.completing());
  }

  @AfterAll
  public static void tearDown(VertxTestContext context) {
    postgresContainer.close();
    network.close();
    context.completeNow();
  }

  @Test
  public void shouldReceiveResponseForQuery1(Vertx vertx, VertxTestContext context) {
    GraphQLTestUtils.verifyQuery(port, 1, vertx, context);
  }


  @Test void shouldReceiveEventsForSubscription1(Vertx vertx, VertxTestContext context) {
    Checkpoint checkpoints = context.checkpoint(2);
    GraphQLTestUtils.verifySubscription(port, 1, 2, vertx, context, checkpoints);
  }
}
