package dev.fastgql.graphql;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.db.MetadataUtils;
import graphql.GraphQL;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import java.sql.Connection;
import java.sql.SQLException;

public class GraphQLFactory {
  public static GraphQL getGraphQL(JsonObject config, Vertx vertx) throws SQLException {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createWithJsonConfig(config.getJsonObject("datasource"));

    DebeziumConfig debeziumConfig =
        DebeziumConfig.createWithJsonConfig(config.getJsonObject("debezium"));

    Connection connection = datasourceConfig.getConnection();
    DatabaseSchema database = MetadataUtils.createDatabaseSchema(connection);
    connection.close();

    Pool client = datasourceConfig.getPool(vertx);

    return GraphQLDefinition.newGraphQL(database, client)
        .enableQuery()
        .enableSubscription(vertx, datasourceConfig, debeziumConfig)
        .build();
  }
}
