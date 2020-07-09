package dev.fastgql.integration;

import dev.fastgql.db.DatasourceConfig;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

public class ContainerEnvWithMySQLDebezium extends AbstractContainerEnvWithDebeziumImpl {
  @Override
  protected JdbcDatabaseContainer<?> createJdbcContainer() {
    return new MySQLContainer<>("fastgql/mysql-testcontainers:latest")
        .withNetwork(network)
        .withNetworkAliases("mysql")
        .withUsername("debezium")
        .withPassword("dbz");
  }

  @Override
  protected ConnectorConfiguration createConnectorConfiguration() {
    return ConnectorConfiguration.forJdbcContainer(jdbcDatabaseContainer)
        .with("database.server.name", "dbserver")
        .with("slot.name", "debezium")
        .with(
            "database.history.kafka.bootstrap.servers",
            String.format("%s:9092", kafkaContainer.getNetworkAliases().get(0)))
        .with(
            "database.history.kafka.topic",
            String.format("schema-changes.%s", jdbcDatabaseContainer.getDatabaseName()));
  }

  @Override
  protected DatasourceConfig createDatasourceConfig() {
    return DatasourceConfig.createDatasourceConfig(
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword(),
        jdbcDatabaseContainer.getDatabaseName());
  }

  @Override
  public String getJdbcUrlForMultipleQueries() {
    return String.format("%s?allowMultiQueries=true", jdbcDatabaseContainer.getJdbcUrl());
  }
}
