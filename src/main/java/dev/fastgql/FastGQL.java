/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.graphql.GraphQLDefinition;
import dev.fastgql.metadata.MetadataUtils;
import dev.fastgql.router.RouterUpdatable;
import graphql.GraphQL;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class FastGQL extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", FastGQL.class.getName());
  }

  @Override
  public void start(Promise<Void> future) throws SQLException {

    Connection connection =
        DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/quarkus_test", "quarkus_test", "quarkus_test");
    DatabaseSchema database = MetadataUtils.createDatabaseSchema(connection);
    connection.close();

    Pool client =
        PgPool.pool(
            vertx,
            new PgConnectOptions()
                .setHost("localhost")
                .setPort(5432)
                .setDatabase("quarkus_test")
                .setUser("quarkus_test")
                .setPassword("quarkus_test"),
            new PoolOptions().setMaxSize(5));

    GraphQL graphQL = GraphQLDefinition.newGraphQL(database, client).enableQuery().build();
    RouterUpdatable routerUpdatable = new RouterUpdatable(vertx);
    routerUpdatable.update(graphQL);

    vertx
        .createHttpServer()
        .requestHandler(routerUpdatable.getRouter())
        .rxListen(config().getInteger("http.port", 8080))
        .doOnSuccess(server -> future.complete())
        .doOnError(server -> future.fail(server.getCause()))
        .subscribe();
  }
}
