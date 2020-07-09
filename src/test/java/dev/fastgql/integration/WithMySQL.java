package dev.fastgql.integration;

import dev.fastgql.db.DatasourceConfig;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

public interface WithMySQL extends WithFastGQL {
  @Override
  default JdbcDatabaseContainer<?> createJdbcDatabaseContainerWithoutNetwork() {
    return new MySQLContainer<>("fastgql/mysql-testcontainers:latest")
        .withNetworkAliases("mysql")
        .withUsername("debezium")
        .withPassword("dbz");
  }

  @Override
  default DatasourceConfig createDatasourceConfig() {
    return DatasourceConfig.createDatasourceConfig(
        getJdbcDatabaseContainer().getJdbcUrl(),
        getJdbcDatabaseContainer().getUsername(),
        getJdbcDatabaseContainer().getPassword(),
        getJdbcDatabaseContainer().getDatabaseName());
  }

  @Override
  default String getJdbcUrlForMultipleQueries() {
    return String.format("%s?allowMultiQueries=true", getJdbcDatabaseContainer().getJdbcUrl());
  }
}
