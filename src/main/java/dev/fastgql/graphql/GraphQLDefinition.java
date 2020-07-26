/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

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
import dev.fastgql.sql.SQLArguments;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Transaction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static Builder newGraphQL(DatabaseSchema databaseSchema, Pool client) {
    return new Builder(databaseSchema, client);
  }

  public static class Builder {
    private final DatabaseSchema databaseSchema;
    private final GraphQLDatabaseSchema graphQLDatabaseSchema;
    private final Pool sqlConnectionPool;
    private final GraphQLSchema.Builder graphQLSchemaBuilder;
    private final GraphQLCodeRegistry.Builder graphQLCodeRegistryBuilder;
    private boolean queryEnabled = false;
    private boolean subscriptionEnabled = false;

    /**
     * Class builder, has to be initialized with database schema and SQL connection pool.
     *
     * @param databaseSchema input database schema
     * @param sqlConnectionPool SQL connection pool
     */
    public Builder(DatabaseSchema databaseSchema, Pool sqlConnectionPool) {
      this.databaseSchema = databaseSchema;
      this.graphQLDatabaseSchema = new GraphQLDatabaseSchema(databaseSchema);
      this.sqlConnectionPool = sqlConnectionPool;
      this.graphQLSchemaBuilder = GraphQLSchema.newSchema();
      this.graphQLCodeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
    }

    private static Single<List<Map<String, Object>>> executeQuery(
        String query, Transaction transaction) {
      return transaction
          .rxQuery(query)
          .map(
              rowSet -> {
                List<String> columnNames = rowSet.columnsNames();
                List<Map<String, Object>> retList = new ArrayList<>();
                rowSet.forEach(
                    row -> {
                      Map<String, Object> r = new HashMap<>();
                      columnNames.forEach(
                          columnName -> r.put(columnName, row.getValue(columnName)));
                      retList.add(r);
                    });
                return retList;
              });
    }

    private static void traverseSelectionSet(
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
                GraphQLFieldDefinition graphQLFieldDefinition =
                    graphQLDatabaseSchema.fieldAt(
                        parent.tableNameWhenParent(), selectedField.getName());
                switch (graphQLFieldDefinition.getReferenceType()) {
                  case NONE:
                    parent.addComponent(
                        new ComponentRow(graphQLFieldDefinition.getQualifiedName().getKeyName()));
                    break;
                  case REFERENCING:
                    Component componentReferencing =
                        new ComponentReferencing(
                            selectedField.getName(),
                            graphQLFieldDefinition.getQualifiedName().getKeyName(),
                            graphQLFieldDefinition.getForeignName().getTableName(),
                            aliasGenerator.getAlias(),
                            graphQLFieldDefinition.getForeignName().getKeyName());
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
                            graphQLFieldDefinition.getQualifiedName().getKeyName(),
                            graphQLFieldDefinition.getForeignName().getTableName(),
                            aliasGenerator.getAlias(),
                            graphQLFieldDefinition.getForeignName().getKeyName(),
                            new SQLArguments(selectedField.getArguments()));
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
          new ExecutionRoot(env.getField().getName(), aliasGenerator.getAlias(), sqlArguments);
      // executionRoot.setSqlExecutor(sqlExecutor);
      traverseSelectionSet(
          graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
      return executionRoot;
    }

    private Single<List<Map<String, Object>>> getResponse(
        DataFetchingEnvironment env, Transaction transaction) {
      ComponentExecutable executionRoot = getExecutionRoot(env);
      executionRoot.setSqlExecutor(queryString -> executeQuery(queryString, transaction));
      return executionRoot.execute();
    }

    private void commitTransaction(Transaction transaction) {
      transaction
          .rxCommit()
          .subscribe(() -> log.debug("transaction committed"), err -> log.error(err.getMessage()));
    }

    /**
     * Enables query by defining query data fetcher using {@link VertxDataFetcher} and adding it to
     * {@link GraphQLCodeRegistry}.
     *
     * @return this
     */
    public Builder enableQuery() {
      if (queryEnabled) {
        return this;
      }
      VertxDataFetcher<List<Map<String, Object>>> queryDataFetcher =
          new VertxDataFetcher<>(
              (env, listPromise) ->
                  sqlConnectionPool
                      .rxBegin()
                      .flatMap(
                          transaction ->
                              getResponse(env, transaction)
                                  .doFinally(() -> commitTransaction(transaction)))
                      .subscribe(listPromise::complete, listPromise::fail));
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
     * Enables subscription by defining subscription data fetcher and adding it to {@link
     * GraphQLCodeRegistry}.
     *
     * @param vertx vertx instance
     * @param datasourceConfig datasource config
     * @param debeziumConfig debezium config
     * @return this
     */
    public Builder enableSubscription(
        Vertx vertx, DatasourceConfig datasourceConfig, DebeziumConfig debeziumConfig) {

      if (subscriptionEnabled || !debeziumConfig.isActive()) {
        log.debug("Subscription already enabled or debezium is not configured");
        return this;
      }

      if (debeziumConfig.isEmbedded()) {
        try {
          DebeziumEngineSingleton.startNewEngine(datasourceConfig, debeziumConfig);
        } catch (IOException e) {
          log.error("subscription not enabled: debezium engine could not start");
          return this;
        }
      }

      DataFetcher<Flowable<List<Map<String, Object>>>> subscriptionDataFetcher =
          env -> {
            log.info("new subscription");
            ComponentExecutable executionRoot = getExecutionRoot(env);
            return EventFlowableFactory.create(
                    executionRoot, vertx, datasourceConfig, debeziumConfig)
                .flatMap(record -> sqlConnectionPool.rxBegin().toFlowable())
                .flatMap(
                    transaction -> {
                      executionRoot.setSqlExecutor(query -> executeQuery(query, transaction));
                      return executionRoot
                          .execute()
                          .doFinally(() -> commitTransaction(transaction))
                          .toFlowable();
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
      GraphQLSchema graphQLSchema =
          graphQLSchemaBuilder.codeRegistry(graphQLCodeRegistryBuilder.build()).build();
      return GraphQL.newGraphQL(graphQLSchema).build();
    }
  }
}
