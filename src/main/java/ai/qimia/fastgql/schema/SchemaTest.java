package ai.qimia.fastgql.schema;

import graphql.GraphQL;
import graphql.schema.*;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SchemaTest extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", SchemaTest.class.getName());
  }

  private void traverseSelectionSet(GraphQLDatabaseSchema graphQLDatabaseSchema, String table, DataFetchingFieldSelectionSet selectionSet) {
    System.out.println("pk -> " + table + "(" + graphQLDatabaseSchema.getPrimaryKeys().get(table) + ")");
    traverseSelectionSetInner(graphQLDatabaseSchema, table, selectionSet);
  }

  private void traverseSelectionSetInner(GraphQLDatabaseSchema graphQLDatabaseSchema, String table, DataFetchingFieldSelectionSet selectionSet) {
    selectionSet.getFields().forEach(field -> {
      // todo: cleaner way to skip non-root nodes?
      if (field.getQualifiedName().contains("/")) {
        return;
      }
      GraphQLNodeDefinition node = graphQLDatabaseSchema.getGraph().get(table).get(field.getName());
      ReferenceType referenceType = node.getReferenceType();
      QualifiedName thisName = node.getQualifiedName();
      String key = thisName.getName();
      if (referenceType.equals(ReferenceType.NONE)) {
        System.out.println(table + "(" + key + ")");
      } else {
        QualifiedName foreignName = node.getForeignName();
        String foreignTable = foreignName.getParent();
        String foreignKey = foreignName.getName();
        System.out.println("traversing " + field.getQualifiedName() + ", " + table + "(" + key + ") -> " + foreignTable + "(" + foreignKey + ")");
        if (referenceType.equals(ReferenceType.REFERENCED)) {
          System.out.println("pk -> " + foreignTable + "(" + graphQLDatabaseSchema.getPrimaryKeys().get(foreignTable) + ")");
        }
        traverseSelectionSetInner(graphQLDatabaseSchema, foreignTable, field.getSelectionSet());
      }
/*
      if (field.getSelectionSet().getFields().size() > 0) {
        QualifiedName foreignName = node.getForeignName();
        String foreignTable = foreignName.getParent();
        String foreignKey = foreignName.getName();
        System.out.println("traversing " + field.getQualifiedName() + ", " + table + "(" + key + ") -> " + foreignTable + "(" + foreignKey + ")");
        traverseSelectionSet(graphQLDatabaseSchema, foreignTable, field.getSelectionSet());
      } else {
        System.out.println(table + "(" + key + ")");
      }
*/
    });
  }

  @Override
  public void start(Promise<Void> future) {
    DatabaseSchema database = DatabaseSchema.newSchema()
      .row("addresses/id", FieldType.INT)
      .row("customers/id", FieldType.INT)
      .row("customers/address", FieldType.INT, "addresses/id")
      .primaryKey("customers", "id")
      .primaryKey("addresses", "id")
      .build();

    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);
    GraphQLObjectType query = queryBuilder.build();

    DataFetcher<String> dataFetcher = env -> {
      traverseSelectionSet(graphQLDatabaseSchema, env.getExecutionStepInfo().getField().getName(), env.getSelectionSet());
/*
      env.getSelectionSet().getFields().forEach(field -> {
        System.out.println(field.getQualifiedName() + " " + field.getSelectionSet().getFields());
      });
*/
      return "none";
    };

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetcher(FieldCoordinates.coordinates("Query", "customers"), (DataFetcher<?>) env -> List.of(Map.of("id", 5)))
      .dataFetcher(FieldCoordinates.coordinates("Query", "addresses"), (DataFetcher<?>) env -> List.of(Map.of("id", 1)))
      .dataFetcher(FieldCoordinates.coordinates("customers", "address_ref"), (DataFetcher<?>) env -> Map.of("id", 7))
      .dataFetcher(FieldCoordinates.coordinates("addresses", "customers_on_address"), (DataFetcher<?>) env -> List.of(Map.of("id", 8)))
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
