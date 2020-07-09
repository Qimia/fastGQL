package dev.fastgql.integration;

import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;

public interface WithExternalDebezium extends WithDebezium {

  ConnectorConfiguration createConnectorConfiguration();

  DebeziumContainer getDebeziumContainer();

  KafkaContainer getKafkaContainer();

  @Override
  default Stream<GenericContainer<?>> getAllContainers() {
    return Stream.of(getKafkaContainer(), getJdbcDatabaseContainer(), getDebeziumContainer());
  }

  @Override
  default void closeAllContainers() {
    getDebeziumContainer().close();
    getKafkaContainer().close();
    getJdbcDatabaseContainer().close();
    getNetwork().close();
  }

  @Override
  default boolean registerConnector(VertxTestContext context) {
    try {
      getDebeziumContainer().registerConnector("my-connector", createConnectorConfiguration());
    } catch (IOException e) {
      context.failNow(e);
      return false;
    }
    return true;
  }

  @Override
  default Map<String, Object> createDebeziumConfigEntry() {
    return Map.of(
        "embedded",
        false,
        "bootstrap.servers",
        getKafkaContainer().getBootstrapServers(),
        "server",
        "dbserver");
  }
}
