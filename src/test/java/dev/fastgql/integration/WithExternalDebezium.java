package dev.fastgql.integration;

import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.vertx.junit5.VertxTestContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.stream.Stream;

public interface WithExternalDebezium extends WithDebezium {

  ConnectorConfiguration createConnectorConfiguration();
  DebeziumContainer getDebeziumContainer();
  KafkaContainer getKafkaContainer();
  Network getNetwork();

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
  default boolean isDebeziumEmbedded() {
    return false;
  }
}
