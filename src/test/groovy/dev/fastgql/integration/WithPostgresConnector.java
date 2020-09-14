package dev.fastgql.integration;

import io.debezium.testing.testcontainers.ConnectorConfiguration;

public interface WithPostgresConnector extends WithExternalDebezium {

  @Override
  default ConnectorConfiguration createConnectorConfiguration() {
    return ConnectorConfiguration.forJdbcContainer(getJdbcDatabaseContainer())
        .with("database.server.name", "dbserver")
        .with("slot.name", "debezium");
  }
}
