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
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.sqlclient.Pool;
import java.util.List;
import java.util.Map;

public class GraphQLDefinition {

  public static GraphQL create(DatabaseSchema database, Pool client) {
    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);

    VertxDataFetcher<List<Map<String, Object>>> vertxDataFetcher =
        new VertxDataFetcher<>(
            (env, listPromise) ->
                client
                    .rxGetConnection()
                    .doOnSuccess(
                        connection -> {
                          AliasGenerator aliasGenerator = new AliasGenerator();
                          ComponentExecutable executionRoot =
                              new ExecutionRoot(
                                  env.getField().getName(),
                                  aliasGenerator.getAlias(),
                                  queryString ->
                                      SQLResponseUtils.executeQuery(queryString, connection));
                          SQLResponseUtils.traverseSelectionSet(
                              connection,
                              graphQLDatabaseSchema,
                              executionRoot,
                              aliasGenerator,
                              env.getSelectionSet());
                          executionRoot
                              .execute()
                              .doOnSuccess(listPromise::complete)
                              .doOnError(listPromise::fail)
                              .doFinally(connection::close)
                              .subscribe();
                        })
                    .doOnError(listPromise::fail)
                    .subscribe());

    GraphQLCodeRegistry.Builder codeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
    database
        .getTableNames()
        .forEach(
            tableName ->
                codeRegistryBuilder.dataFetcher(
                    FieldCoordinates.coordinates("Query", tableName), vertxDataFetcher));

    GraphQLSchema graphQLSchema =
        GraphQLSchema.newSchema()
            .query(queryBuilder.build())
            .codeRegistry(codeRegistryBuilder.build())
            .build();

    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
