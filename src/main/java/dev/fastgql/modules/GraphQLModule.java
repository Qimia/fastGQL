package dev.fastgql.modules;

import dagger.Module;
import dagger.Provides;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.events.DebeziumEngineSingleton;
import dev.fastgql.events.EventFlowableFactory;
import dev.fastgql.graphql.GraphQLDefinition;
import dev.fastgql.sql.SQLExecutor;
import graphql.GraphQL;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.inject.Singleton;

@Module
public abstract class GraphQLModule {

  @Provides
  @Singleton
  static Supplier<GraphQL> provideGraphQLSupplier(
      Vertx vertx,
      DebeziumConfig debeziumConfig,
      Pool sqlConnectionPool,
      DatasourceConfig datasourceConfig,
      Supplier<DatabaseSchema> databaseSchemaSupplier,
      Function<Transaction, SQLExecutor> transactionSQLExecutorFunction,
      DebeziumEngineSingleton debeziumEngineSingleton,
      EventFlowableFactory eventFlowableFactory) {
    return () ->
        new GraphQLDefinition.Builder(
                sqlConnectionPool,
                datasourceConfig,
                databaseSchemaSupplier,
                transactionSQLExecutorFunction,
                debeziumEngineSingleton,
                eventFlowableFactory)
            .enableQuery()
            .enableSubscription(vertx, debeziumConfig)
            .enableMutation()
            .build();
  }
}
