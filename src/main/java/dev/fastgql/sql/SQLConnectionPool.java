/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.db.DatasourceConfig;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class SQLConnectionPool {

  public static Pool createWithConfiguration(DatasourceConfig datasourceConfig, Vertx vertx) {

      return PgPool.pool(
            vertx,
            new PgConnectOptions()
                .setHost(datasourceConfig.getHost())
                .setPort(datasourceConfig.getPort())
                .setDatabase(datasourceConfig.getDb())
                .setUser(datasourceConfig.getUsername())
                .setPassword(datasourceConfig.getPassword()),
            new PoolOptions().setMaxSize(5));
  }
}
