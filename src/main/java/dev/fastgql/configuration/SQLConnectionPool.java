/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.configuration;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.PoolOptions;

public class SQLConnectionPool {

    public Pool createWithConfiguration(JsonObject config, Vertx vertx) {

        JsonObject datasourceConfig = config.getJsonObject("datasource");

        Pool client = PgPool.pool(
                vertx,
                new PgConnectOptions()
                        .setHost(datasourceConfig.getString("host"))
                        .setPort(datasourceConfig.getInteger("port"))
                        .setDatabase(datasourceConfig.getString("db"))
                        .setUser(datasourceConfig.getString("username"))
                        .setPassword(datasourceConfig.getString("password")),
                new PoolOptions().setMaxSize(5));

        return client;
    }
}
