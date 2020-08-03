package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.db.MetadataUtils;
import dev.fastgql.graphql.GraphQLDefinition;
import graphql.GraphQL;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Supplier;

public class GraphQLModule extends AbstractModule {

  @Provides
  Function<Connection, DatabaseSchema> provideConnectionDatabaseSchemaFunction() {
    return connection -> {
      DatabaseSchema databaseSchema;
      try {
        databaseSchema = MetadataUtils.createDatabaseSchema(connection);
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
      return databaseSchema;
    };
  }

  @Provides
  DatasourceConfig provideDatasourceConfig(JsonObject config) {
    return DatasourceConfig.createWithJsonConfig(config.getJsonObject("datasource"));
  }

  @Provides
  DebeziumConfig provideDebeziumConfig(JsonObject config) {
    return DebeziumConfig.createWithJsonConfig(config.getJsonObject("debezium"));
  }

  @Provides
  Pool providePool(DatasourceConfig datasourceConfig, Vertx vertx) {
    return datasourceConfig.getPool(vertx);
  }

  @Provides
  Supplier<GraphQL> provideGraphQLSupplier(DatasourceConfig datasourceConfig, DebeziumConfig debeziumConfig, Function<Connection, DatabaseSchema> connectionDatabaseSchemaFunction, Pool sqlConnectionPool, Vertx vertx) {
    return () -> {
      DatabaseSchema databaseSchema;
      try {
        Connection connection = datasourceConfig.getConnection();
        databaseSchema = connectionDatabaseSchemaFunction.apply(connection);
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
      return GraphQLDefinition.newGraphQL(databaseSchema, sqlConnectionPool, datasourceConfig.getDbType())
        .enableQuery()
        .enableSubscription(vertx, datasourceConfig, debeziumConfig)
        .enableMutation()
        .build();
    };
  }
}
