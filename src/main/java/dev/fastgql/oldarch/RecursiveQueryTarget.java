package dev.fastgql.oldarch;

import graphql.GraphQL;
import graphql.schema.*;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

public class RecursiveQueryTarget extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", RecursiveQueryTarget.class.getName());
  }

  @Override
  public void start(Promise<Void> future) {

    GraphQLObjectType customersList = GraphQLObjectType.newObject()
      .name("customers")
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("id")
        .type(GraphQLInt)
        .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("first_name")
        .type(GraphQLString)
        .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("address_id")
        .type(GraphQLInt)
        .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("address_id_ref")
        .type(GraphQLTypeReference.typeRef("addresses"))
        .build())
      .build();

    GraphQLObjectType addressesList = GraphQLObjectType.newObject()
      .name("addresses")
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("id")
        .type(GraphQLInt)
        .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("street")
        .type(GraphQLString)
        .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("customers_on_address_id")
        .type(GraphQLList.list(GraphQLTypeReference.typeRef("customers")))
        .build())
      .build();

    GraphQLObjectType query = GraphQLObjectType.newObject()
      .name("Query")
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("customers")
        .type(GraphQLList.list(customersList))
        .build())
      .field(GraphQLFieldDefinition.newFieldDefinition()
        .name("addresses")
        .type(GraphQLList.list(addressesList))
        .build())
      .build();

/*
    DataFetcher<String> dataFetcher = env -> "value";
    DataFetcher<Map<String, Object>> recursiveDataFetcher = env -> {
      Map<String, Object> returnMap = new HashMap<>();
      System.out.println("***********");
      env.getSelectionSet().getFields().forEach(field -> {
        System.out.println(field.getQualifiedName() + " " + field.getSelectionSet().getFields());
      });
*/
/*
      env.getSelectionSet().getFields().forEach(selectedField -> {
        System.out.println(selectedField.getSelectionSet().getFields().for);
      });
*/
/*
      return Map.of("field", Map.of("value", "recursed"));
    };
*/

/*
    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetcher(FieldCoordinates.coordinates("Query", "field2"), dataFetcher)
      .dataFetcher(FieldCoordinates.coordinates("Query", "field"), recursiveDataFetcher)
      .build();
*/

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(query)
/*
      .codeRegistry(codeRegistry)
*/
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
