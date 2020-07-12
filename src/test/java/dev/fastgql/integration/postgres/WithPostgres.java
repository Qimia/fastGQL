/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration.postgres;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.integration.WithFastGQL;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public interface WithPostgres extends WithFastGQL {
  @Override
  default JdbcDatabaseContainer<?> createJdbcDatabaseContainerWithoutNetwork() {
    return new PostgreSQLContainer<>(getDatabaseDockerImageName()).withNetworkAliases("postgres");
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
