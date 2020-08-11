package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.graphql.GraphQLDefinition;
import graphql.GraphQL;
import io.vertx.reactivex.core.Vertx;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Supplier;

public class GraphQLModule extends AbstractModule {

  @Override
  protected void configure() {
    install(
        new FactoryModuleBuilder()
            .implement(GraphQLDefinition.Builder.class, GraphQLDefinition.DefaultBuilder.class)
            .build(GraphQLDefinition.BuilderFactory.class));
  }

  @Provides
  Supplier<GraphQL> provideGraphQLSupplier(
      Vertx vertx,
      DatasourceConfig datasourceConfig,
      DebeziumConfig debeziumConfig,
      Function<Connection, DatabaseSchema> connectionDatabaseSchemaFunction,
      GraphQLDefinition.BuilderFactory graphQLDefinitionBuilderFactory) {
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

      return graphQLDefinitionBuilderFactory
          .create(databaseSchema)
          .enableQuery()
          .enableSubscription(vertx, debeziumConfig)
          .enableMutation()
          .build();
    };
  }
}
