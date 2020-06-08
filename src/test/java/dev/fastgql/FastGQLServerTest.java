/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import static dev.fastgql.GraphQLTestUtils.verifyQuerySimple;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

@ExtendWith(VertxExtension.class)
public class FastGQLServerTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(FastGQLServerTest.class);
  private static final int port = 8081;
  private static Network network = Network.newNetwork();
  private static KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);
  private static PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("debezium/postgres:11")
          .withNetwork(network)
          .withNetworkAliases("postgres");
  private static DebeziumContainer debeziumContainer =
      new DebeziumContainer("1.0")
          .withNetwork(network)
          .withKafka(kafkaContainer)
          .withLogConsumer(new Slf4jLogConsumer(LOGGER))
          .dependsOn(kafkaContainer);
  private final int customersStartOffset = 5;
  private String deploymentID;

  private void setUpContainers(Vertx vertx, VertxTestContext context) {
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
              .with("slot.name", "debezium"));
    } catch (IOException e) {
      context.failNow(e);
      return;
    }

    JsonObject config =
        new JsonObject()
            .put("http.port", port)
            .put("bootstrap.servers", kafkaContainer.getBootstrapServers())
            .put("datasource", JsonObject.mapFrom(DBTestUtils.datasourceConfig(postgresContainer)));

    DeploymentOptions options = new DeploymentOptions().setConfig(config);
    vertx
        .rxDeployVerticle(new FastGQL(), options)
        .doOnSuccess(
            deploymentID1 -> {
              deploymentID = deploymentID1;
              context.completeNow();
            })
        .subscribe();
  }

  private void tearDownContainers(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(deploymentID)
        .doOnComplete(
            () -> {
              debeziumContainer.close();
              kafkaContainer.close();
              postgresContainer.close();
              network.close();
              context.completeNow();
            })
        .subscribe();
  }

  @Nested
  @DisplayName("Query Tests")
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

    @Test
    public void shouldReceiveResponseForSimpleQuery(Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/simple/select-addresses", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForSimpleNestedCustomerAddressQuery(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/simple/select-nested-customer-address", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForSimpleNestedAddressCustomerQuery(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/simple/select-nested-address-customer", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForSimpleNestedAddressCustomerAddressQuery(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/simple/select-nested-address-customer-address", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForSimpleNestedCustomerAddressCustomerQuery(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/simple/select-nested-customer-address-customer", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForLimitAddressesLimit1Query(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/limit/select-addresses-limit-1", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForLimitAddressesLimit2Query(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/limit/select-addresses-limit-2", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForOffsetCustomersLimit2Offset0(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/offset/select-customers-limit-2-offset-0", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForOffsetCustomersLimit2Offset1(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/offset/select-customers-limit-2-offset-1", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForOffsetNestedAddressCustomerLimit2Offset1(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/offset/select-nested-address-customer-limit-2-offset-1", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForOrderByAddressesOrderByStreetDesc(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/order-by/select-addresses-order-by-street-desc", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForOrderByCustomersOrderByFirstNameAsc(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/order-by/select-customers-order-by-first-name-asc", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForOrderByNestedAddressCustomerOrderByFirstNameDesc(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/order-by/select-nested-address-customer-order-by-first-name-desc",
          port,
          vertx,
          context);
    }

    @Test
    public void shouldReceiveResponseForWhereAddressesWhereIdEq101(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/where/select-addresses-where-id-eq-101", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForWhereAddressesWhereIdGt101(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple("queries/where/select-addresses-where-id-gt-101", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForWhereAddressesWhereStreetEqAstreet(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/where/select-addresses-where-street-eq-Astreet", port, vertx, context);
    }

    @Test
    public void shouldReceiveResponseForWhereNestedCustomerAddressWhereStreetEqAstreet(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/where/select-nested-customer-address-where-street-eq-Astreet",
          port,
          vertx,
          context);
    }

    @Test
    public void shouldReceiveResponseForWhereNestedAddressCustomerWhereIdGt103(
        Vertx vertx, VertxTestContext context) {
      verifyQuerySimple(
          "queries/where/select-nested-address-customer-where-id-gt-103", port, vertx, context);
    }
  }

  @Nested
  @DisplayName("Subscription Tests")
  class SubscriptionTests {
    @BeforeEach
    public void setUp(Vertx vertx, VertxTestContext context) {
      setUpContainers(vertx, context);
    }

    @AfterEach
    public void tearDown(Vertx vertx, VertxTestContext context) {
      tearDownContainers(vertx, context);
    }

    @Test
    void shouldReceiveEventsForSimpleSubscription(Vertx vertx, VertxTestContext context) {
      String query = "subscription/simple/select-addresses/query.graphql";
      List<String> expected =
          List.of(
              "subscription/simple/select-addresses/expected-1.json",
              "subscription/simple/select-addresses/expected-2.json");
      GraphQLTestUtils.verifySubscription(
          port, query, expected, customersStartOffset, vertx, context);
      DBTestUtils.executeSQLQueryWithDelay(
          "INSERT INTO customers VALUES (107, 'John', 'Qwe', 'john@qwe.com', 101)",
          1000,
          TimeUnit.MILLISECONDS,
          postgresContainer,
          context);
    }
  }
}
