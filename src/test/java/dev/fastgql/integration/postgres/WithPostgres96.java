/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration.postgres;

public interface WithPostgres96 extends WithPostgres {

  @Override
  default String getDatabaseDockerImageName() {
    return "debezium/postgres:9.6-alpine";
  }
}
