package dev.fastgql.integration;

import dev.fastgql.db.DatasourceConfig;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class ContainerEnvWithPostgresDebezium extends AbstractContainerEnvWithDebeziumImpl {
  @Override
  protected JdbcDatabaseContainer<?> createJdbcContainer() {
    return new PostgreSQLContainer<>("debezium/postgres:11")
        .withNetwork(network)
        .withNetworkAliases("postgres");
  }

  @Override
  protected ConnectorConfiguration createConnectorConfiguration() {
    return ConnectorConfiguration.forJdbcContainer(jdbcDatabaseContainer)
        .with("database.server.name", "dbserver")
        .with("slot.name", "debezium");
  }

  @Override
  protected DatasourceConfig createDatasourceConfig() {
    return DatasourceConfig.createDatasourceConfig(
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword(),
        "public");
  }

  @Override
  public String getJdbcUrlForMultipleQueries() {
    return jdbcDatabaseContainer.getJdbcUrl();
  }
}
