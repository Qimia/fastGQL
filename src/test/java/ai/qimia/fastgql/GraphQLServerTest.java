package ai.qimia.fastgql;

import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import rx.Observable;

@ExtendWith(VertxExtension.class)
@SuppressWarnings("ResultOfMethodCallIgnored")
public class GraphQLServerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(GraphQLServerTest.class);
  private String deploymentID;

  private static final int port = 8081;

  private static Network network = Network.newNetwork();

  private static KafkaContainer kafkaContainer = new KafkaContainer()
      .withNetwork(network);

  private static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
      "debezium/postgres:11")
      .withNetwork(network)
      .withNetworkAliases("postgres");

  private static DebeziumContainer debeziumContainer = new DebeziumContainer("1.0")
      .withNetwork(network)
      .withKafka(kafkaContainer)
      .withLogConsumer(new Slf4jLogConsumer(LOGGER))
      .dependsOn(kafkaContainer);

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) {

    Startables.deepStart(Stream.of(kafkaContainer, postgresContainer, debeziumContainer)).join();

    try {
      DBTestUtils.executeSQLQueryFromResource("init.sql", postgresContainer);
    } catch (SQLException | IOException e) {
      context.failNow(e);
      return;
    }

    try {
      debeziumContainer.registerConnector(
          "my-connector",
          ConnectorConfiguration.forJdbcContainer(postgresContainer)
              .with("database.server.name", "dbserver")
              .with("slot.name", "debezium")
      );
    } catch (IOException e) {
      context.failNow(e);
      return;
    }

    JsonObject config = new JsonObject()
        .put("http.port", port)
        .put("bootstrap.servers", kafkaContainer.getBootstrapServers())
        .put("datasource", JsonObject.mapFrom(DBTestUtils.datasourceConfig(postgresContainer)));

    DeploymentOptions options = new DeploymentOptions()
        .setConfig(config);
    vertx.rxDeployVerticle(new GraphQLServer(), options).subscribe(
        deploymentID -> {
          this.deploymentID = deploymentID;
          context.completeNow();
        }
    );
  }

  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) {
    vertx.rxUndeploy(deploymentID)
        .subscribe(
            () -> {
              debeziumContainer.close();
              kafkaContainer.close();
              postgresContainer.close();
              network.close();
              context.completeNow();
            }
        );
  }

  @Test
  public void shouldReceiveResponseForQuery1(Vertx vertx, VertxTestContext context) {
    GraphQLTestUtils.verifyQuery(port, 1, vertx, context);
  }

  @Test
  void shouldReceiveEventsForSubscription1(Vertx vertx, VertxTestContext context) {

    String inputResource = "test-subscription-input-1.graphql";
    List<String> outputResources = List.of(
        "test-subscription-output-1-1.json",
        "test-subscription-output-1-1.json",
        "test-subscription-output-1-2.json"
    );
    GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);

    Observable.timer(5, TimeUnit.SECONDS)
        .subscribe(
            result -> {
              try {
                DBTestUtils.executeSQLQuery(
                    "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
                    postgresContainer);
              } catch (SQLException e) {
                context.failNow(e);
              }
            }
        );
  }

  @Test
  public void shouldReceiveResponseForQueryWithLimitOffset(Vertx vertx, VertxTestContext context) {
    String inputResource = "test-limit-offset/test-query-input.graphql";
    String outputResource = "test-limit-offset/test-query-output.json";
    GraphQLTestUtils.verifyQuery(port, inputResource, outputResource, vertx, context);
  }

  @Test
  void shouldReceiveEventsForSubscriptionWithLimitOffset(Vertx vertx, VertxTestContext context) {
    String inputResource = "test-limit-offset/test-subscription-input.graphql";
    List<String> outputResources = List.of(
        "test-limit-offset/test-subscription-output-1.json",
        "test-limit-offset/test-subscription-output-1.json",
        "test-limit-offset/test-subscription-output-2.json"
    );
    GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);

    Observable.timer(5, TimeUnit.SECONDS)
        .subscribe(
            result -> {
              try {
                DBTestUtils.executeSQLQuery(
                    "DELETE FROM customers WHERE id = 101",
                    postgresContainer);
              } catch (SQLException e) {
                context.failNow(e);
              }
            }
        );
  }

  @Test
  public void shouldReceiveResponseForQueryWithOrderBy(Vertx vertx, VertxTestContext context) {
    String inputResource = "test-order-by/test-query-input.graphql";
    String outputResource = "test-order-by/test-query-output.json";
    GraphQLTestUtils.verifyQuery(port, inputResource, outputResource, vertx, context);
  }

  @Test
  void shouldReceiveEventsForSubscriptionWithOrderBy(Vertx vertx, VertxTestContext context) {
    String inputResource = "test-order-by/test-subscription-input.graphql";
    List<String> outputResources = List.of(
        "test-order-by/test-subscription-output-1.json",
        "test-order-by/test-subscription-output-1.json",
        "test-order-by/test-subscription-output-2.json"
    );
    GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);

    Observable.timer(5, TimeUnit.SECONDS)
        .subscribe(
            result -> {
              try {
                DBTestUtils.executeSQLQuery(
                    "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
                    postgresContainer);
              } catch (SQLException e) {
                context.failNow(e);
              }
            }
        );
  }
}
