package dev.fastgql.modules;

import dagger.Module;
import dagger.Provides;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.graphql.GraphQLDefinition_BuilderFactory;
import graphql.GraphQL;
import io.vertx.reactivex.core.Vertx;
import java.util.function.Supplier;
import javax.inject.Singleton;

@Module
public abstract class GraphQLModule {

  @Provides
  @Singleton
  static Supplier<GraphQL> provideGraphQLSupplier(
      Vertx vertx,
      DebeziumConfig debeziumConfig,
      Supplier<DatabaseSchema> databaseSchemaSupplier,
      GraphQLDefinition_BuilderFactory graphQLDefinition_builderFactory) {
    return () -> {
      DatabaseSchema databaseSchema = databaseSchemaSupplier.get();
      if (databaseSchema == null) {
        return null;
      }
      return graphQLDefinition_builderFactory
          .create(databaseSchema)
          .enableQuery()
          .enableSubscription(vertx, debeziumConfig)
          .enableMutation()
          .build();
    };
  }
}
