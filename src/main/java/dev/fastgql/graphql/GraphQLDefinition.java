/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.graphql;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.sql.*;
import graphql.GraphQL;
import graphql.schema.*;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.SqlConnection;

import java.util.*;
import java.util.function.Function;

public class GraphQLDefinition {

  public static Builder newGraphQL(DatabaseSchema databaseSchema, Pool client) {
    return new Builder(databaseSchema, client);
  }

  public static class Builder {
    private final DatabaseSchema databaseSchema;
    private final GraphQLDatabaseSchema graphQLDatabaseSchema;
    private final Pool client;
    private final GraphQLSchema.Builder graphQLSchemaBuilder;
    private final GraphQLCodeRegistry.Builder graphQLCodeRegistryBuilder;
    private boolean queryEnabled = false;
    private boolean subscriptionEnabled = false;

    public Builder(DatabaseSchema databaseSchema, Pool client) {
      this.databaseSchema = databaseSchema;
      this.graphQLDatabaseSchema = new GraphQLDatabaseSchema(databaseSchema);
      this.client = client;
      this.graphQLSchemaBuilder = GraphQLSchema.newSchema();
      this.graphQLCodeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
    }

    private static Single<List<Map<String, Object>>> executeQuery(
        String query, SqlConnection connection) {
      return connection
          .rxQuery(query)
          .map(
              rowSet -> {
                List<String> columnNames = rowSet.columnsNames();
                List<Map<String, Object>> rList = new ArrayList<>();
                rowSet.forEach(
                    row -> {
                      Map<String, Object> r = new HashMap<>();
                      columnNames.forEach(
                          columnName -> r.put(columnName, row.getValue(columnName)));
                      rList.add(r);
                    });
                return rList;
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
              field -> {
                if (field.getQualifiedName().contains("/")) {
                  return;
                }
                GraphQLNodeDefinition node =
                    graphQLDatabaseSchema.nodeAt(parent.trueTableNameWhenParent(), field.getName());
                switch (node.getReferenceType()) {
                  case NONE:
                    parent.addComponent(new ComponentRow(node.getQualifiedName().getName()));
                    break;
                  case REFERENCING:
                    Component componentReferencing =
                        new ComponentReferencing(
                            field.getName(),
                            node.getQualifiedName().getName(),
                            node.getForeignName().getParent(),
                            aliasGenerator.getAlias(),
                            node.getForeignName().getName());
                    traverseSelectionSet(
                        graphQLDatabaseSchema,
                        componentReferencing,
                        aliasGenerator,
                        field.getSelectionSet());
                    parent.addComponent(componentReferencing);
                    break;
                  case REFERENCED:
                    Component componentReferenced =
                        new ComponentReferenced(
                            field.getName(),
                            node.getQualifiedName().getName(),
                            node.getForeignName().getParent(),
                            aliasGenerator.getAlias(),
                            node.getForeignName().getName());
                    traverseSelectionSet(
                        graphQLDatabaseSchema,
                        componentReferenced,
                        aliasGenerator,
                        field.getSelectionSet());
                    parent.addComponent(componentReferenced);
                    break;
                  default:
                    throw new RuntimeException("Unrecognized reference type");
                }
              });
    }

    private ComponentExecutable getExecutionRoot(DataFetchingEnvironment env, SqlExecutor sqlExecutor) {
      AliasGenerator aliasGenerator = new AliasGenerator();
      ComponentExecutable executionRoot =
        new ExecutionRoot(
          env.getField().getName(),
          aliasGenerator.getAlias());
      executionRoot.setSqlExecutor(sqlExecutor);
      traverseSelectionSet(
        graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
      return executionRoot;
    }

    private Single<List<Map<String, Object>>> getResponse(
        DataFetchingEnvironment env, SqlConnection connection) {
      SqlExecutor sqlExecutor = new SqlExecutor();
      ComponentExecutable executionRoot = getExecutionRoot(env, sqlExecutor);
      sqlExecutor.setSqlExecutorFunction(queryString -> executeQuery(queryString, connection));
      return executionRoot.execute();
    }

    public Builder enableQuery() {
      if (queryEnabled) {
        return this;
      }
      VertxDataFetcher<List<Map<String, Object>>> queryDataFetcher =
          new VertxDataFetcher<>(
              (env, listPromise) ->
                  client
                      .rxGetConnection()
                      .doOnSuccess(
                          connection ->
                              getResponse(env, connection)
                                  .doOnSuccess(listPromise::complete)
                                  .doOnError(listPromise::fail)
                                  .doFinally(connection::close)
                                  .subscribe())
                      .doOnError(listPromise::fail)
                      .subscribe());
      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates("Query", tableName), queryDataFetcher));
      queryEnabled = true;
      return this;
    }

    public Builder enableSubscription(Flowable<String> modifiedTablesStream) {
      if (subscriptionEnabled) {
        return this;
      }
      DataFetcher<Flowable<List<Map<String, Object>>>> subscriptionDataFetcher = env -> {
        SqlExecutor sqlExecutor = new SqlExecutor();
        ComponentExecutable executionRoot = getExecutionRoot(env, sqlExecutor);
        Set<String> queriedTables = executionRoot.getQueriedTables();
        return modifiedTablesStream
          .filter(queriedTables::contains)
          .flatMap(table -> client.rxGetConnection().toFlowable())
          .flatMap(connection -> {
            sqlExecutor.setSqlExecutorFunction(query -> executeQuery(query, connection));
            return executionRoot.execute().doFinally(connection::close).toFlowable();
          });
      };
/*
      DataFetcher<Flowable<List<Map<String, Object>>>> subscriptionDataFetcher =
          env ->
              modifiedTablesStream
                  .filter(table -> databaseSchema.getTableNames().contains(table))
                  .flatMap(table -> client.rxGetConnection().toFlowable())
                  .flatMap(
                      connection ->
                          getResponse(env, connection).doFinally(connection::close).toFlowable());
*/
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
