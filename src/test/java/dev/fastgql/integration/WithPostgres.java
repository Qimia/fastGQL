package dev.fastgql.integration;

import dev.fastgql.db.DatasourceConfig;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public interface WithPostgres extends WithFastGQL {
  @Override
  default JdbcDatabaseContainer<?> createJdbcDatabaseContainerWithoutNetwork() {
    return new PostgreSQLContainer<>("debezium/postgres:11").withNetworkAliases("postgres");
  }

  @Override
  default DatasourceConfig createDatasourceConfig() {
    return DatasourceConfig.createDatasourceConfig(
        getJdbcDatabaseContainer().getJdbcUrl(),
        getJdbcDatabaseContainer().getUsername(),
        getJdbcDatabaseContainer().getPassword(),
        "public");
  }

  @Override
  default String getJdbcUrlForMultipleQueries() {
    return getJdbcDatabaseContainer().getJdbcUrl();
  }
}
