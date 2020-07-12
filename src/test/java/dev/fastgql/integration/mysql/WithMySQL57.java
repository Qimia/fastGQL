/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration.mysql;

public interface WithMySQL57 extends WithMySQL {

  @Override
  default String getDatabaseDockerImageName() {
    return "fastgql/mysql-testcontainers:5.7";
  }
}
