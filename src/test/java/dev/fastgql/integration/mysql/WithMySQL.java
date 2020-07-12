/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration.mysql;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.integration.WithFastGQL;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

public interface WithMySQL extends WithFastGQL {
  @Override
  default JdbcDatabaseContainer<?> createJdbcDatabaseContainerWithoutNetwork() {
    return new MySQLContainer<>(getDatabaseDockerImageName())
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
