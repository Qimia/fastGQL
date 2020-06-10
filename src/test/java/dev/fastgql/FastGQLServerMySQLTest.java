/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql;

import dev.fastgql.db.DatasourceConfig;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

@ExtendWith(VertxExtension.class)
public class FastGQLServerMySQLTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(FastGQLServerMySQLTest.class);
  private String deploymentID;

  private static final int port = 8081;

  private static Network network = Network.newNetwork();

  private static KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);

  private static MySQLContainer<?> mysqlContainer =
      new MySQLContainer<>("fastgql/mysql-testcontainers:latest")
          .withNetwork(network)
          .withNetworkAliases("mysql")
          .withUsername("debezium")
          .withPassword("dbz");

  private static DebeziumContainer debeziumContainer =
      new DebeziumContainer("1.0")
          .withNetwork(network)
          .withKafka(kafkaContainer)
          .withLogConsumer(new Slf4jLogConsumer(LOGGER))
          .dependsOn(kafkaContainer);

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) {

    Startables.deepStart(Stream.of(kafkaContainer, mysqlContainer, debeziumContainer)).join();

    try {
      DBTestUtils.executeSQLQueryFromResource("init.sql", mysqlContainer);
    } catch (SQLException | IOException e) {
      context.failNow(e);
      return;
    }

    try {
      debeziumContainer.registerConnector(
          "my-connector",
          ConnectorConfiguration.forJdbcContainer(mysqlContainer)
              .with("database.server.name", "dbserver")
              .with("slot.name", "debezium")
              .with("database.history.kafka.bootstrap.servers", String.format("%s:9092", kafkaContainer.getNetworkAliases().get(0)))
              .with("database.history.kafka.topic", String.format("schema-changes.%s", mysqlContainer.getDatabaseName()))
      );
    } catch (IOException e) {
      context.failNow(e);
      return;
    }

    DatasourceConfig datasourceConfig = DBTestUtils.datasourceConfig(mysqlContainer);

    JsonObject config =
        new JsonObject()
            .put("http.port", port)
            .put("bootstrap.servers", kafkaContainer.getBootstrapServers())
            .put(
                "datasource",
                Map.of(
                    "jdbcUrl", datasourceConfig.getJdbcUrl(),
                    "username", datasourceConfig.getUsername(),
                    "password", datasourceConfig.getPassword(),
                    "schema", datasourceConfig.getSchema()));

    DeploymentOptions options = new DeploymentOptions().setConfig(config);
    vertx
        .rxDeployVerticle(new FastGQL(), options)
        .doOnSuccess(
            deploymentID -> {
              this.deploymentID = deploymentID;
              context.completeNow();
            })
        .subscribe();
  }

  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(deploymentID)
        .doOnComplete(
            () -> {
              debeziumContainer.close();
              kafkaContainer.close();
              mysqlContainer.close();
              network.close();
              context.completeNow();
            })
        .subscribe();
  }

  @Test
  public void shouldReceiveResponseForSimpleQuery(Vertx vertx, VertxTestContext context) {
    String inputResource = "test-simple/test-query-input.graphql";
    String outputResource = "test-simple/test-query-output.json";
    GraphQLTestUtils.verifyQuery(port, inputResource, outputResource, vertx, context);
  }

  @Test
  void shouldReceiveEventsForSimpleSubscription(Vertx vertx, VertxTestContext context) {
    String inputResource = "test-simple/test-subscription-input.graphql";
    List<String> outputResources =
        List.of(
            "test-simple/test-subscription-output-1.json",
            "test-simple/test-subscription-output-1.json",
            "test-simple/test-subscription-output-2.json");
    GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);
    DBTestUtils.executeSQLQueryWithDelay(
        "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
        5,
        TimeUnit.SECONDS,
        mysqlContainer,
        context);
  }

  /*
    @Test
    public void shouldReceiveResponseForQueryWithLimitOffset(Vertx vertx, VertxTestContext context) {
      String inputResource = "test-limit-offset/test-query-input.graphql";
      String outputResource = "test-limit-offset/test-query-output.json";
      GraphQLTestUtils.verifyQuery(port, inputResource, outputResource, vertx, context);
    }

    @Test
    void shouldReceiveEventsForSubscriptionWithLimitOffset(Vertx vertx, VertxTestContext context) {
      String inputResource = "test-limit-offset/test-subscription-input.graphql";
      List<String> outputResources =
          List.of(
              "test-limit-offset/test-subscription-output-1.json",
              "test-limit-offset/test-subscription-output-1.json",
              "test-limit-offset/test-subscription-output-2.json");
      GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);
      DBTestUtils.executeSQLQueryWithDelay(
          "DELETE FROM customers WHERE id = 101", 5, TimeUnit.SECONDS, postgresContainer, context);
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
      List<String> outputResources =
          List.of(
              "test-order-by/test-subscription-output-1.json",
              "test-order-by/test-subscription-output-1.json",
              "test-order-by/test-subscription-output-2.json");
      GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);
      DBTestUtils.executeSQLQueryWithDelay(
          "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
          5,
          TimeUnit.SECONDS,
          postgresContainer,
          context);
    }

    @Test
    public void shouldReceiveResponseForQueryWithWhere(Vertx vertx, VertxTestContext context) {
      String inputResource = "test-where/test-query-input.graphql";
      String outputResource = "test-where/test-query-output.json";
      GraphQLTestUtils.verifyQuery(port, inputResource, outputResource, vertx, context);
    }

    @Test
    void shouldReceiveEventsForSubscriptionWithWhere(Vertx vertx, VertxTestContext context) {
      String inputResource = "test-where/test-subscription-input.graphql";
      List<String> outputResources =
          List.of(
              "test-where/test-subscription-output-1.json",
              "test-where/test-subscription-output-1.json",
              "test-where/test-subscription-output-2.json");
      GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);
      DBTestUtils.executeSQLQueryWithDelay(
          "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
          5,
          TimeUnit.SECONDS,
          postgresContainer,
          context);
    }

    @Test
    public void shouldReceiveResponseForQueryWithDistinctOn(Vertx vertx, VertxTestContext context) {
      DBTestUtils.executeSQLQuery(
          "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
          postgresContainer,
          context);
      String inputResource = "test-distinct-on/test-query-input.graphql";
      String outputResource = "test-distinct-on/test-query-output.json";
      GraphQLTestUtils.verifyQuery(port, inputResource, outputResource, vertx, context);
    }

    @Test
    void shouldReceiveEventsForSubscriptionWithDistinctOn(Vertx vertx, VertxTestContext context) {
      String inputResource = "test-distinct-on/test-subscription-input.graphql";
      List<String> outputResources =
          List.of(
              "test-distinct-on/test-subscription-output-1.json",
              "test-distinct-on/test-subscription-output-1.json",
              "test-distinct-on/test-subscription-output-2.json");
      GraphQLTestUtils.verifySubscription(port, inputResource, outputResources, vertx, context);
      DBTestUtils.executeSQLQueryWithDelay(
          "INSERT INTO customers VALUES (103, 'John', 'Qwe', 'john@qwe.com')",
          5,
          TimeUnit.SECONDS,
          postgresContainer,
          context);
    }
  */
}
