/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.MetadataUtils;
import dev.fastgql.graphql.GraphQLDefinition;
import dev.fastgql.router.RouterUpdatable;
import graphql.GraphQL;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.sqlclient.Pool;
import java.sql.Connection;
import java.sql.SQLException;

public class FastGQL extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", FastGQL.class.getName(), "--conf", "src/main/conf.json");
  }

  @Override
  public void start(Promise<Void> future) throws SQLException {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createWithJsonConfig(config().getJsonObject("datasource"));

    Connection connection = datasourceConfig.getConnection();
    DatabaseSchema database = MetadataUtils.createDatabaseSchema(connection);
    connection.close();

    Pool client = datasourceConfig.getPool(vertx);

    GraphQL graphQL =
        GraphQLDefinition.newGraphQL(database, client)
            .enableQuery()
            .enableSubscription(
                vertx,
                config().getString("bootstrap.servers"),
                "dbserver",
                datasourceConfig.getSchema())
            .build();

    RouterUpdatable routerUpdatable = RouterUpdatable.createWithQueryAndSubscription(vertx);
    routerUpdatable.update(graphQL);

    vertx
        .createHttpServer(new HttpServerOptions().setWebsocketSubProtocols("graphql-ws"))
        .requestHandler(routerUpdatable.getRouter())
        .rxListen(config().getInteger("http.port", 8080))
        .doOnSuccess(server -> future.complete())
        .doOnError(server -> future.fail(server.getCause()))
        .subscribe();
  }
}
