package ai.qimia.fastgql.graphql;

import ai.qimia.fastgql.db.DatabaseSchema;
import ai.qimia.fastgql.sql.AliasGenerator;
import ai.qimia.fastgql.sql.ComponentExecutable;
import ai.qimia.fastgql.sql.ExecutionRoot;
import ai.qimia.fastgql.sql.SQLResponseUtils;
import graphql.GraphQL;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.sqlclient.Pool;

import java.util.List;
import java.util.Map;

public class GraphQLUtils {
  public static GraphQL create(DatabaseSchema database, Pool client) {
    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);
    GraphQLObjectType query = queryBuilder.build();


    VertxDataFetcher<List<Map<String, Object>>> vertxDataFetcher = new VertxDataFetcher<>(((env, listPromise) -> {
      AliasGenerator aliasGenerator = new AliasGenerator();
      ComponentExecutable executionRoot = new ExecutionRoot(env.getField().getName(), aliasGenerator.getAlias(), queryString -> SQLResponseUtils.executeQuery(queryString, client));
      SQLResponseUtils.traverseSelectionSet(client, graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
      executionRoot.execute().subscribe(listPromise::complete);
    }));

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetcher(FieldCoordinates.coordinates("Query", "customers"), vertxDataFetcher)
      .build();

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(query)
      .codeRegistry(codeRegistry)
      .build();

    return GraphQL.newGraphQL(graphQLSchema).build();
  }
}
