package ai.qimia.fastgql.schema;

import graphql.GraphQL;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

public class SchemaTest extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", SchemaTest.class.getName());
  }

  @Override
  public void start(Promise<Void> future) {
    DatabaseSchema database = new DatabaseSchema();

    database.addRowDefinition("customers", "id", FieldType.INT);
    database.addRowDefinition("customers", "address", FieldType.INT, "addresses", "id");
    database.addRowDefinition("addresses", "id", FieldType.INT);

    GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject()
      .name("Query");
    database.addToGraphQLObjectType(queryBuilder);
    GraphQLObjectType query = queryBuilder.build();

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(query)
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
