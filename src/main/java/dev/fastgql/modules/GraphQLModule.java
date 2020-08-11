package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.graphql.GraphQLDefinition;
import graphql.GraphQL;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import java.sql.Connection;
import java.util.function.Function;

public class GraphQLModule extends AbstractModule {

  @Override
  protected void configure() {
    install(
        new FactoryModuleBuilder()
            .implement(GraphQLDefinition.Builder.class, GraphQLDefinition.DefaultBuilder.class)
            .build(GraphQLDefinition.BuilderFactory.class));
  }

  @Provides
  Single<GraphQL> provideGraphQLSingle(
      Vertx vertx,
      DatasourceConfig datasourceConfig,
      DebeziumConfig debeziumConfig,
      Function<Connection, DatabaseSchema> connectionDatabaseSchemaFunction,
      GraphQLDefinition.BuilderFactory graphQLDefinitionBuilderFactory) {
    return Single.fromCallable(
            () -> {
              DatabaseSchema databaseSchema;
              Connection connection = datasourceConfig.getConnection();
              databaseSchema = connectionDatabaseSchemaFunction.apply(connection);
              connection.close();
              return databaseSchema;
            })
        .subscribeOn(Schedulers.io())
        .map(
            databaseSchema ->
                graphQLDefinitionBuilderFactory
                    .create(databaseSchema)
                    .enableQuery()
                    .enableSubscription(vertx, debeziumConfig)
                    .enableMutation()
                    .build());
  }
}
