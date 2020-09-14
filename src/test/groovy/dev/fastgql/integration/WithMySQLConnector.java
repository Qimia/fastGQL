package dev.fastgql.integration;

import io.debezium.testing.testcontainers.ConnectorConfiguration;

public interface WithMySQLConnector extends WithExternalDebezium {

  @Override
  default ConnectorConfiguration createConnectorConfiguration() {
    return ConnectorConfiguration.forJdbcContainer(getJdbcDatabaseContainer())
        .with("database.server.name", "dbserver")
        .with("slot.name", "debezium")
        .with(
            "database.history.kafka.bootstrap.servers",
            String.format("%s:9092", getKafkaContainer().getNetworkAliases().get(0)))
        .with(
            "database.history.kafka.topic",
            String.format("schema-changes.%s", getJdbcDatabaseContainer().getDatabaseName()));
  }
}
