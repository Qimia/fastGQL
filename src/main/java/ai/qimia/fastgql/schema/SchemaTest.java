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
    List<String> keysToQuery = new ArrayList<>();
    selectionSet.getFields().forEach(field -> {
      // todo: cleaner way to skip non-root nodes?
      if (field.getQualifiedName().contains("/")) {
        return;
      }
      GraphQLNodeDefinition node = graphQLDatabaseSchema.getGraph().get(table).get(field.getName());
      ReferenceType referenceType = node.getReferenceType();
      String key = node.getQualifiedName().getName();
      if (referenceType.equals(ReferenceType.NONE)) {
        System.out.println(table + "(" + key + ")");
        keysToQuery.add(key);
      } else {
        QualifiedName foreignName = node.getForeignName();
        String foreignTable = foreignName.getParent();
        String foreignKey = foreignName.getName();
        System.out.println("traversing " + field.getQualifiedName() + ", " + table + "(" + key + ") -> " + foreignTable + "(" + foreignKey + ")");
/*
        if (referenceType.equals(ReferenceType.REFERENCED)) {
          System.out.println("pk -> " + foreignTable + "(" + graphQLDatabaseSchema.getPrimaryKeys().get(foreignTable) + ")");
        }
*/
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
    //System.out.println(keysToQuery);
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

    DataFetcher<List<Map<String, Object>>> queryCustomersDataFetcher = env -> {
      // return all rows
      // SELECT field1, field2, ... from customers
      traverseSelectionSetInner(graphQLDatabaseSchema, "customers", env.getSelectionSet());
      return List.of(Map.of("id", 5));
    };

    DataFetcher<Map<String, Object>> customersAddressRefDataFetcher = env -> {
      // get address for specific address id
      // SELECT field1, field2, ... from addresses where id = 100
      //System.out.println(env.getExecutionStepInfo());
      return Map.of("id", 7);
    };

    DataFetcher<List<Map<String, Object>>> addressesCustomersOnAddressDataFetcher = env -> {
      // get customers matching specific address id
      // SELECT field1, field2, ... from customers where address = 100
      return List.of(Map.of("id", 8));
    };

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetcher(FieldCoordinates.coordinates("Query", "customers"), queryCustomersDataFetcher)
      .dataFetcher(FieldCoordinates.coordinates("Query", "addresses"), (DataFetcher<?>) env -> List.of(Map.of("id", 1)))
      .dataFetcher(FieldCoordinates.coordinates("customers", "address_ref"), customersAddressRefDataFetcher)
      .dataFetcher(FieldCoordinates.coordinates("addresses", "customers_on_address"), addressesCustomersOnAddressDataFetcher)
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
