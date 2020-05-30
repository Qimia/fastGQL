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

import java.util.List;

public class SchemaTest extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", SchemaTest.class.getName());
  }

  private void traverseSelectionSet(DataFetchingFieldSelectionSet selectionSet) {
    selectionSet.getFields().forEach(field -> {
      // todo: cleaner way to skip non-root nodes?
      if (field.getQualifiedName().contains("/")) {
        return;
      }
      if (field.getSelectionSet().getFields().size() > 0) {
        System.out.println("traversing " + field.getQualifiedName());
        traverseSelectionSet(field.getSelectionSet());
      } else {
        System.out.println(field.getQualifiedName());
      }
    });
  }

  private void traverseFields(List<SelectedField> fields) {
    fields.forEach(field -> {
      if (field.getSelectionSet().getFields().size() > 0) {
        System.out.println("traversing " + field.getQualifiedName());
        traverseFields(field.getSelectionSet().getFields());
      } else {
        System.out.println(field.getQualifiedName());
      }
    });
  }


  @Override
  public void start(Promise<Void> future) {
    DatabaseSchema database = DatabaseSchema.newSchema()
      .row("addresses/id", FieldType.INT)
      .row("addresses/id2", FieldType.INT)
      .row("customers/id", FieldType.INT)
      .row("customers/address", FieldType.INT, "addresses/id")
      .row("customers/address2", FieldType.INT, "addresses/id2")
      .build();

    GraphQLDatabaseSchema graphQLDatabaseSchema = new GraphQLDatabaseSchema(database);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    graphQLDatabaseSchema.applyToGraphQLObjectType(queryBuilder);
    GraphQLObjectType query = queryBuilder.build();

    DataFetcher<String> dataFetcher = env -> {
      traverseSelectionSet(env.getSelectionSet());
/*
      env.getSelectionSet().getFields().forEach(field -> {
        System.out.println(field.getQualifiedName() + " " + field.getSelectionSet().getFields());
      });
*/
      return "none";
    };

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetcher(FieldCoordinates.coordinates("Query", "customers"), dataFetcher)
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
