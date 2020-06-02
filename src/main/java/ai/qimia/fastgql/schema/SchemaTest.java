package ai.qimia.fastgql.schema;

import ai.qimia.fastgql.schema.sql.*;
import graphql.GraphQL;
import graphql.schema.*;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import java.util.List;
import java.util.Map;

public class SchemaTest extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", SchemaTest.class.getName());
  }

  private void traverseSelectionSet(Pool client, GraphQLDatabaseSchema graphQLDatabaseSchema, ComponentParent parent, AliasGenerator aliasGenerator, DataFetchingFieldSelectionSet selectionSet) {
    selectionSet.getFields().forEach(field -> {
      // todo: cleaner way to skip non-root nodes?
      if (field.getQualifiedName().contains("/")) {
        return;
      }
      GraphQLNodeDefinition node = graphQLDatabaseSchema.nodeAt(parent.trueTableNameWhenParent(), field.getName());
      switch (node.getReferenceType()) {
        case NONE:
          parent.addComponent(new ComponentRow(node.getQualifiedName().getName()));
          break;
        case REFERENCING:
          Component componentReferencing = new ComponentReferencing(
            field.getName(),
            node.getQualifiedName().getName(),
            node.getForeignName().getParent(),
            aliasGenerator.getAlias(),
            node.getForeignName().getName()
          );
          traverseSelectionSet(client, graphQLDatabaseSchema, componentReferencing, aliasGenerator, field.getSelectionSet());
          parent.addComponent(componentReferencing);
          break;
        case REFERENCED:
          Component componentReferenced = new ComponentReferenced(
            field.getName(),
            node.getQualifiedName().getName(),
            node.getForeignName().getParent(),
            aliasGenerator.getAlias(),
            node.getForeignName().getName(),
            queryString -> SQLResponseProcessor.executeQuery(queryString, client)
          );
          traverseSelectionSet(client, graphQLDatabaseSchema, componentReferenced, aliasGenerator, field.getSelectionSet());
          parent.addComponent(componentReferenced);
          break;
        default:
          throw new RuntimeException("Unrecognized reference type");
      }
    });
  }

  @Override
  public void start(Promise<Void> future) {
    DatabaseSchema database = DatabaseSchema.newSchema()
      .row("customers/id", FieldType.INT)
      .row("customers/first_name", FieldType.STRING)
      .row("customers/last_name", FieldType.STRING)
      .row("customers/email", FieldType.STRING)
      .row("customers/address", FieldType.INT, "addresses/id")
      .row("addresses/id", FieldType.INT)
      .row("addresses/street", FieldType.STRING)
      .row("addresses/house_number", FieldType.INT)
      .build();

    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);
    GraphQLObjectType query = queryBuilder.build();

    Pool client = PgPool.pool(
      vertx,
      new PgConnectOptions()
        .setHost("localhost")
        .setPort(5432)
        .setDatabase("quarkus_test")
        .setUser("quarkus_test")
        .setPassword("quarkus_test"),
      new PoolOptions().setMaxSize(5)
    );

    VertxDataFetcher<List<Map<String, Object>>> vertxDataFetcher = new VertxDataFetcher<>(((env, listPromise) -> {
      AliasGenerator aliasGenerator = new AliasGenerator();
      ComponentExecutable executionRoot = new ExecutionRoot(env.getField().getName(), aliasGenerator.getAlias(), queryString -> SQLResponseProcessor.executeQuery(queryString, client));
      traverseSelectionSet(client, graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
      executionRoot.execute().subscribe(listPromise::complete);
    }));

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetcher(FieldCoordinates.coordinates("Query", "customers"), vertxDataFetcher)
      .build();

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(query)
      .codeRegistry(codeRegistry)
      .build();

    GraphQL graphQL = GraphQL.newGraphQL(graphQLSchema).build();

    Router router = Router.router(vertx);
    router.route("/graphql").handler(GraphQLHandler.create(graphQL));
    router.route("/graphiql/*").handler(GraphiQLHandler.create(
      new GraphiQLHandlerOptions().setEnabled(true)
    ));

    vertx
      .createHttpServer()
      .requestHandler(router)
      .rxListen(config().getInteger("http.port", 8080))
      .subscribe(server -> future.complete(), server -> future.fail(server.getCause()));
  }
}
