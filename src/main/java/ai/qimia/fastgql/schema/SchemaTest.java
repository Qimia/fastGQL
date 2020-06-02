package ai.qimia.fastgql.schema;

import ai.qimia.fastgql.schema.sql.*;
import graphql.GraphQL;
import graphql.schema.*;
import io.reactivex.Single;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

import java.util.List;
import java.util.Map;

public class SchemaTest extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", SchemaTest.class.getName());
  }

  private void traverseSelectionSet(GraphQLDatabaseSchema graphQLDatabaseSchema, ComponentParent parent, AliasGenerator aliasGenerator, DataFetchingFieldSelectionSet selectionSet) {
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
          traverseSelectionSet(graphQLDatabaseSchema, componentReferencing, aliasGenerator, field.getSelectionSet());
          parent.addComponent(componentReferencing);
          break;
        case REFERENCED:
          Component componentReferenced = new ComponentReferenced(
            field.getName(),
            node.getQualifiedName().getName(),
            node.getForeignName().getParent(),
            aliasGenerator.getAlias(),
            node.getForeignName().getName()
          );
          traverseSelectionSet(graphQLDatabaseSchema, componentReferenced, aliasGenerator, field.getSelectionSet());
          List<Map<String, Object>> forged = List.of(
            Map.of(
              "a2_id", 105
            ),
            Map.of(
              "a2_id", 106
            )
          );
          ((ComponentExecutable) componentReferenced).setForgedResponse(Single.just(forged));
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
      .row("addresses/id", FieldType.INT)
      .row("customers/id", FieldType.INT)
      .row("customers/address", FieldType.INT, "addresses/id")
      .build();

    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);
    GraphQLObjectType query = queryBuilder.build();

    VertxDataFetcher<List<Map<String, Object>>> vertxDataFetcher = new VertxDataFetcher<>(((env, listPromise) -> {
      AliasGenerator aliasGenerator = new AliasGenerator();
      ComponentExecutable executionRoot = new ExecutionRoot(env.getField().getName(), aliasGenerator.getAlias());
      traverseSelectionSet(graphQLDatabaseSchema, executionRoot, aliasGenerator, env.getSelectionSet());
      List<Map<String, Object>> forged = List.of(
        Map.of(
          "a0_id", 101,
          "a0_address", 102,
          "a1_id", 102
        )
      );
      executionRoot.setForgedResponse(Single.just(forged));
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
