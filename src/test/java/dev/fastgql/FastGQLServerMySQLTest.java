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
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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
  private String deploymentID;

  private void setUpContainers(Vertx vertx, VertxTestContext context) {
    TestUtils.closeContainers(kafkaContainer, mysqlContainer, debeziumContainer, network);
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
              .with(
                  "database.history.kafka.bootstrap.servers",
                  String.format("%s:9092", kafkaContainer.getNetworkAliases().get(0)))
              .with(
                  "database.history.kafka.topic",
                  String.format("schema-changes.%s", mysqlContainer.getDatabaseName())));
    } catch (IOException e) {
      context.failNow(e);
      return;
    }

    DatasourceConfig datasourceConfig = DBTestUtils.datasourceConfig(mysqlContainer);

    DeploymentOptions options =
        TestUtils.getDeploymentOptions(datasourceConfig, kafkaContainer, port);
    vertx
        .rxDeployVerticle(new FastGQL(), options)
        .doOnSuccess(
            deploymentID -> {
              this.deploymentID = deploymentID;
              context.completeNow();
            })
        .subscribe();
  }

  private void tearDownContainers(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(deploymentID)
        .doOnComplete(
            () -> {
              TestUtils.closeContainers(kafkaContainer, mysqlContainer, debeziumContainer, network);
              context.completeNow();
            })
        .subscribe();
  }

  @Nested
  @DisplayName("MySQL Query Tests")
  @TestInstance(Lifecycle.PER_CLASS)
  class QueryTests {
    @BeforeAll
    public void setUp(Vertx vertx, VertxTestContext context) {
      setUpContainers(vertx, context);
    }

    @AfterAll
    public void tearDown(Vertx vertx, VertxTestContext context) {
      tearDownContainers(vertx, context);
    }

    @ParameterizedTest(name = "{index} => Test: [{arguments}]")
    @MethodSource("dev.fastgql.TestUtils#queryDirectories")
    void shouldReceiveResponse(String directory, Vertx vertx, VertxTestContext context) {
      System.out.println(String.format("Test: %s", directory));
      GraphQLTestUtils.verifyQuerySimple(directory, port, vertx, context);
    }
  }

  @Nested
  @DisplayName("MySQL Subscription Tests")
  class SubscriptionTests {
    @BeforeEach
    public void setUp(Vertx vertx, VertxTestContext context) {
      setUpContainers(vertx, context);
    }

    @AfterEach
    public void tearDown(Vertx vertx, VertxTestContext context) {
      tearDownContainers(vertx, context);
    }

    @ParameterizedTest(name = "{index} => Test: [{arguments}]")
    @MethodSource("dev.fastgql.TestUtils#subscriptionDirectories")
    public void shoudReceiveResponse(String directory, Vertx vertx, VertxTestContext context) {
      GraphQLTestUtils.verifySubscriptionSimple(
          directory, port, 5, TimeUnit.SECONDS, mysqlContainer, vertx, context);
    }
  }
}
