/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import com.google.inject.assistedinject.Assisted;
import dev.fastgql.common.TableWithAlias;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.events.DebeziumEngineSingleton;
import dev.fastgql.events.EventFlowableFactory;
import dev.fastgql.sql.AliasGenerator;
import dev.fastgql.sql.Component;
import dev.fastgql.sql.ComponentExecutable;
import dev.fastgql.sql.ComponentParent;
import dev.fastgql.sql.ComponentReferenced;
import dev.fastgql.sql.ComponentReferencing;
import dev.fastgql.sql.ComponentRow;
import dev.fastgql.sql.ExecutionRoot;
import dev.fastgql.sql.MutationExecution;
import dev.fastgql.sql.SQLArguments;
import dev.fastgql.sql.SQLExecutor;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.SelectedField;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.SqlConnection;
import io.vertx.reactivex.sqlclient.Transaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to build {@link GraphQL} from {@link DatabaseSchema} (used for defining GraphQL schema) and
 * SQL connection pool ({@link Pool} - used in data fetchers).
 *
 * @author Kamil Bobrowski
 */
public class GraphQLDefinition {

  private static final Logger log = LoggerFactory.getLogger(GraphQLDefinition.class);

  public interface BuilderFactory {
    Builder create(DatabaseSchema databaseSchema);
  }

  public interface Builder {
    Builder enableQuery();

    Builder enableSubscription(Vertx vertx, DebeziumConfig debeziumConfig);

    Builder enableMutation();

    GraphQL build();
  }

  public static class DefaultBuilder implements Builder {
    private final DatabaseSchema databaseSchema;
    private final GraphQLDatabaseSchema graphQLDatabaseSchema;
    private final Pool sqlConnectionPool;
    private final GraphQLSchema.Builder graphQLSchemaBuilder;
    private final GraphQLCodeRegistry.Builder graphQLCodeRegistryBuilder;
    private final Function<Transaction, SQLExecutor> transactionSQLExecutorFunction;
    private final Function<SqlConnection, SQLExecutor> sqlConnectionSQLExecutorFunction;
    private final DebeziumEngineSingleton debeziumEngineSingleton;
    private final EventFlowableFactory eventFlowableFactory;
    private final Function<Set<TableWithAlias>, String> lockQueryFunction;
    private final String unlockQuery;
    private boolean queryEnabled = false;
    private boolean mutationEnabled = false;
    private boolean subscriptionEnabled = false;
    private boolean returningStatementEnabled = false;

    /**
     * Class builder, has to be initialized with database schema and SQL connection pool.
     *
     * @param databaseSchema input database schema
     * @param sqlConnectionPool SQL connection pool
     */
    @Inject
    public DefaultBuilder(
        @Assisted DatabaseSchema databaseSchema,
        Pool sqlConnectionPool,
        DatasourceConfig datasourceConfig,
        Function<Transaction, SQLExecutor> transactionSQLExecutorFunction,
        Function<SqlConnection, SQLExecutor> sqlConnectionSQLExecutorFunction,
        DebeziumEngineSingleton debeziumEngineSingleton,
        EventFlowableFactory eventFlowableFactory) {
      this.databaseSchema = databaseSchema;
      this.graphQLDatabaseSchema = new GraphQLDatabaseSchema(databaseSchema);
      this.sqlConnectionPool = sqlConnectionPool;
      this.graphQLSchemaBuilder = GraphQLSchema.newSchema();
      this.graphQLCodeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
      if (datasourceConfig.getDbType().equals(DatasourceConfig.DBType.postgresql)) {
        returningStatementEnabled = true;
      }
      this.transactionSQLExecutorFunction = transactionSQLExecutorFunction;
      this.sqlConnectionSQLExecutorFunction = sqlConnectionSQLExecutorFunction;
      this.debeziumEngineSingleton = debeziumEngineSingleton;
      this.eventFlowableFactory = eventFlowableFactory;
      this.lockQueryFunction = datasourceConfig.getLockQueryFunction();
      this.unlockQuery = datasourceConfig.getUnlockQuery();
    }

    private void traverseSelectionSet(
        GraphQLDatabaseSchema graphQLDatabaseSchema,
        ComponentParent parent,
        AliasGenerator aliasGenerator,
        DataFetchingFieldSelectionSet selectionSet) {
      selectionSet
          .getFields()
          .forEach(
              selectedField -> {
                if (selectedField.getQualifiedName().contains("/")) {
                  return;
                }
                GraphQLField graphQLField =
                    graphQLDatabaseSchema.fieldAt(
                        parent.tableNameWhenParent(), selectedField.getName());
                switch (graphQLField.getReferenceType()) {
                  case NONE:
                    parent.addComponent(
                        new ComponentRow(graphQLField.getQualifiedName().getKeyName()));
                    break;
                  case REFERENCING:
                    Component componentReferencing =
                        new ComponentReferencing(
                            selectedField.getName(),
                            graphQLField.getQualifiedName().getKeyName(),
                            graphQLField.getForeignName().getTableName(),
                            aliasGenerator.getAlias(),
                            graphQLField.getForeignName().getKeyName());
                    traverseSelectionSet(
                        graphQLDatabaseSchema,
                        componentReferencing,
                        aliasGenerator,
                        selectedField.getSelectionSet());
                    parent.addComponent(componentReferencing);
                    break;
                  case REFERENCED:
                    Component componentReferenced =
                        new ComponentReferenced(
                            selectedField.getName(),
                            graphQLField.getQualifiedName().getKeyName(),
                            graphQLField.getForeignName().getTableName(),
                            aliasGenerator.getAlias(),
                            graphQLField.getForeignName().getKeyName(),
                            new SQLArguments(selectedField.getArguments()),
                            lockQueryFunction,
                            unlockQuery);
                    traverseSelectionSet(
                        graphQLDatabaseSchema,
                        componentReferenced,
                        aliasGenerator,
                        selectedField.getSelectionSet());
                    parent.addComponent(componentReferenced);
                    break;
                  default:
                    throw new RuntimeException("Unrecognized reference type");
                }
              });
    }

    private ComponentExecutable getExecutionRoot(DataFetchingEnvironment env) {
      AliasGenerator aliasGenerator = new AliasGenerator();
      SQLArguments sqlArguments = new SQLArguments(env.getArguments());
      ComponentExecutable executionRoot =
          new ExecutionRoot(
              env.getField().getName(),
              aliasGenerator.getAlias(),
              sqlArguments,
              lockQueryFunction,
              unlockQuery);
      traverseSelectionSet(
          graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
      return executionRoot;
    }

    private Single<List<Map<String, Object>>> getResponse(
        DataFetchingEnvironment env, Transaction transaction) {
      ComponentExecutable executionRoot = getExecutionRoot(env);
      executionRoot.setSqlExecutor(transactionSQLExecutorFunction.apply(transaction));
      return executionRoot.execute(true);
    }

    private Single<Map<String, Object>> getResponseMutation(
        DataFetchingEnvironment env, Transaction transaction) {
      String fieldName = env.getField().getName();
      Object rowsObject = env.getArgument("objects");
      JsonArray rows = rowsObject == null ? null : new JsonArray((List<?>) rowsObject);
      SelectedField returning = env.getSelectionSet().getField("returning");
      List<String> returningColumns = new ArrayList<>();
      if (returning != null) {
        returning
            .getSelectionSet()
            .getFields()
            .forEach(selectedField -> returningColumns.add(selectedField.getName()));
      }
      return MutationExecution.createResponse(
          transaction, databaseSchema, fieldName, rows, returningColumns);
    }

    /**
     * Enables query by defining data fetcher using {@link VertxDataFetcher} and adding it to {@link
     * GraphQLCodeRegistry}.
     *
     * @return this
     */
    public Builder enableQuery() {
      if (queryEnabled) {
        return this;
      }
      VertxDataFetcher<List<Map<String, Object>>> queryDataFetcher =
          new VertxDataFetcher<>(
              (env, promise) ->
                  sqlConnectionPool
                      .rxBegin()
                      .flatMap(
                          transaction ->
                              getResponse(env, transaction)
                                  .flatMap(
                                      result ->
                                          transaction
                                              .rxCommit()
                                              .doOnComplete(() -> log.info("transaction commited"))
                                              .andThen(Single.just(result))))
                      .subscribe(promise::complete, promise::fail));
      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates("Query", tableName), queryDataFetcher));
      queryEnabled = true;
      return this;
    }

    /**
     * Enables mutation by defining data fetcher using {@link VertxDataFetcher} and adding it to
     * {@link GraphQLCodeRegistry}.
     *
     * @return this
     */
    public Builder enableMutation() {
      if (mutationEnabled) {
        return this;
      }

      VertxDataFetcher<Map<String, Object>> mutationDataFetcher =
          new VertxDataFetcher<>(
              (env, promise) ->
                  sqlConnectionPool
                      .rxBegin()
                      .flatMap(
                          transaction ->
                              getResponseMutation(env, transaction)
                                  .flatMap(
                                      result ->
                                          transaction.rxCommit().andThen(Single.just(result))))
                      .subscribe(promise::complete, promise::fail));

      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates(
                          "Mutation", String.format("insert_%s", tableName)),
                      mutationDataFetcher));

      mutationEnabled = true;
      return this;
    }

    /**
     * Enables subscription by defining subscription data fetcher and adding it to {@link
     * GraphQLCodeRegistry}.
     *
     * @param vertx vertx instance
     * @param debeziumConfig debezium config
     * @return this
     */
    public Builder enableSubscription(Vertx vertx, DebeziumConfig debeziumConfig) {

      if (subscriptionEnabled || !debeziumConfig.isActive()) {
        log.debug("Subscription already enabled or debezium is not configured");
        return this;
      }

      if (debeziumConfig.isEmbedded()) {
        try {
          debeziumEngineSingleton.startNewEngine();
        } catch (IOException e) {
          log.error("subscription not enabled: debezium engine could not start");
          return this;
        }
      }

      DataFetcher<Flowable<List<Map<String, Object>>>> subscriptionDataFetcher =
          env -> {
            log.info("new subscription");
            ComponentExecutable executionRoot = getExecutionRoot(env);
            return eventFlowableFactory
                .create(executionRoot)
                .doOnEach(evt -> System.out.println("EVENT"))
                .flatMap(record -> sqlConnectionPool.rxGetConnection().toFlowable())
                .flatMap(
                    connection -> {
                      SQLExecutor sqlExecutor = sqlConnectionSQLExecutorFunction.apply(connection);
                      executionRoot.setSqlExecutor(sqlExecutor);
                      return connection.rxQuery("SELECT 1").toFlowable().flatMap(r -> executionRoot
                          .execute(true)
                          .flatMap(result -> sqlExecutor.execute("UNLOCK TABLES").map(unlockResult -> result))
                          .flatMap(
                              result -> sqlExecutor.execute("SELECT 1")//connection.rxQuery("UNLOCK TABLES")
                                .flatMap(rows -> connection.rxQuery("SELECT 1"))
                                .flatMap(rows -> {
                                  connection.close();
                                  System.out.println("CONNECTION CLOSED");
                                  return Single.just(result);
                                }))
                          .toFlowable());
                    });
          };
      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates("Subscription", tableName),
                      subscriptionDataFetcher));
      subscriptionEnabled = true;
      return this;
    }

    /**
     * Build {@link GraphQL} by applying internally constructed {@link GraphQLDatabaseSchema} to
     * query / subscription builders.
     *
     * @return constructed GraphQL object
     */
    public GraphQL build() {
      if (!(queryEnabled || subscriptionEnabled)) {
        throw new RuntimeException("query or subscription has to be enabled");
      }
      GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
      GraphQLObjectType.Builder subscriptionBuilder =
          GraphQLObjectType.newObject().name("Subscription");
      GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Mutation");
      List<GraphQLObjectType.Builder> builders = new ArrayList<>();
      if (queryEnabled) {
        builders.add(queryBuilder);
      }
      if (subscriptionEnabled) {
        builders.add(subscriptionBuilder);
      }
      graphQLDatabaseSchema.applyToGraphQLObjectTypes(builders);
      if (queryEnabled) {
        graphQLSchemaBuilder.query(queryBuilder);
      }
      if (subscriptionEnabled) {
        graphQLSchemaBuilder.subscription(subscriptionBuilder);
      }
      if (mutationEnabled) {
        graphQLDatabaseSchema.applyMutation(mutationBuilder, returningStatementEnabled);
        graphQLSchemaBuilder.mutation(mutationBuilder);
      }
      GraphQLSchema graphQLSchema =
          graphQLSchemaBuilder.codeRegistry(graphQLCodeRegistryBuilder.build()).build();
      return GraphQL.newGraphQL(graphQLSchema).build();
    }
  }
}
