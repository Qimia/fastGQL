package dev.fastgql.integration;

import dev.fastgql.db.DatasourceConfig;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class ContainerEnvWithPostgres extends AbstractContainerEnvImpl {
  @Override
  protected JdbcDatabaseContainer<?> createJdbcContainer() {
    return new PostgreSQLContainer<>("debezium/postgres:11").withNetworkAliases("postgres");
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
