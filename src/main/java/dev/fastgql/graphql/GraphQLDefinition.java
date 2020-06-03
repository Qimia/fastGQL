/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.graphql;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.sql.AliasGenerator;
import dev.fastgql.sql.ComponentExecutable;
import dev.fastgql.sql.ExecutionRoot;
import dev.fastgql.sql.SQLResponseUtils;
import graphql.GraphQL;
import graphql.schema.*;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.sqlclient.Pool;

import java.util.List;
import java.util.Map;

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

    public Builder enableQuery() {
      if (queryEnabled) {
        return this;
      }
      GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
        .name("Query");
      graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);
      VertxDataFetcher<List<Map<String, Object>>> vertxDataFetcher = new VertxDataFetcher<>((env, listPromise) -> client
        .rxGetConnection()
        .doOnSuccess(
          connection -> {
            AliasGenerator aliasGenerator = new AliasGenerator();
            ComponentExecutable executionRoot = new ExecutionRoot(
              env.getField().getName(),
              aliasGenerator.getAlias(),
              queryString -> SQLResponseUtils.executeQuery(queryString, connection)
            );
            SQLResponseUtils.traverseSelectionSet(connection, graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
            executionRoot
              .execute()
              .doOnSuccess(listPromise::complete)
              .doOnError(listPromise::fail)
              .doFinally(connection::close)
              .subscribe();
          })
        .doOnError(listPromise::fail)
        .subscribe()
      );
      databaseSchema.getTableNames().forEach(tableName -> graphQLCodeRegistryBuilder.dataFetcher(FieldCoordinates.coordinates("Query", tableName), vertxDataFetcher));
      graphQLSchemaBuilder.query(queryBuilder.build());
      queryEnabled = true;
      return this;
    }

    public Builder enableSubscription() {
      if (subscriptionEnabled) {
        return this;
      }
      return this;
    }

    public GraphQL build() {
      if (!(queryEnabled || subscriptionEnabled)) {
        throw new RuntimeException("query or subscription has to be enabled");
      }
      GraphQLSchema graphQLSchema = graphQLSchemaBuilder.codeRegistry(graphQLCodeRegistryBuilder.build()).build();
      return GraphQL.newGraphQL(graphQLSchema).build();
    }
  }

  public static GraphQL create(DatabaseSchema database, Pool client) {
    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);

    VertxDataFetcher<List<Map<String, Object>>> vertxDataFetcher = new VertxDataFetcher<>((env, listPromise) -> client
      .rxGetConnection()
      .doOnSuccess(
        connection -> {
          AliasGenerator aliasGenerator = new AliasGenerator();
          ComponentExecutable executionRoot = new ExecutionRoot(
            env.getField().getName(),
            aliasGenerator.getAlias(),
            queryString -> SQLResponseUtils.executeQuery(queryString, connection)
          );
          SQLResponseUtils.traverseSelectionSet(connection, graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
          executionRoot
            .execute()
            .doOnSuccess(listPromise::complete)
            .doOnError(listPromise::fail)
            .doFinally(connection::close)
            .subscribe();
        })
      .doOnError(listPromise::fail)
      .subscribe()
    );

    GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
    database.getTableNames().forEach(tableName -> codeRegistryBuilder.dataFetcher(FieldCoordinates.coordinates("Query", tableName), vertxDataFetcher));

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(queryBuilder.build())
      .codeRegistry(codeRegistryBuilder.build())
      .build();

    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
