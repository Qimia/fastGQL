/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import static dev.fastgql.sql.SQLConnectionPool.createWithConfiguration;

import com.google.common.collect.Iterables;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.MetadataUtils;
import dev.fastgql.graphql.GraphQLDefinition;
import dev.fastgql.kafka.KafkaConfig;
import dev.fastgql.router.RouterUpdatable;
import graphql.GraphQL;
import io.reactivex.Flowable;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.sqlclient.Pool;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

public class FastGQL extends AbstractVerticle {

  private static final String alteredTablesTopic = "altered-tables";

  public static void main(String[] args) {
    Launcher.executeCommand(
        "run", FastGQL.class.getName(), "--conf", "src/main/resources/config/vertx/conf.json");
  }

  private static String kafkaMessageToTableName(String message) {
    return Iterables.getLast(
        Arrays.asList(message.substring(1, message.length() - 1).split("\\.")));
  }

  @Override
  public void start(Promise<Void> future) throws SQLException {

    DatasourceConfig datasourceConfig = DatasourceConfig.createDatasourceConfig(config().getJsonObject("datasource"));

    Connection connection =
            DriverManager.getConnection(
                    datasourceConfig.getJdbcUrl(),
                    datasourceConfig.getUsername(),
                    datasourceConfig.getPassword());
    DatabaseSchema database = MetadataUtils.createDatabaseSchema(connection);
    connection.close();

    Pool client = createWithConfiguration(datasourceConfig, vertx);

    KafkaConsumer<String, String> kafkaConsumer = KafkaConsumer.create(vertx, KafkaConfig.createConfigMap(config()));
    kafkaConsumer.subscribe(alteredTablesTopic);

    Flowable<String> alteredTablesFlowable =
        kafkaConsumer.toFlowable().map(record -> kafkaMessageToTableName(record.value()));

    GraphQL graphQL =
        GraphQLDefinition.newGraphQL(database, client)
            .enableQuery()
            .enableSubscription(alteredTablesFlowable)
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
