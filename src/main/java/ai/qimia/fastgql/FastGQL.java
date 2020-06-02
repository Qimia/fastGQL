package ai.qimia.fastgql;

import ai.qimia.fastgql.common.FieldType;
import ai.qimia.fastgql.db.DatabaseSchema;
import ai.qimia.fastgql.graphql.GraphQLUtils;
import graphql.GraphQL;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class FastGQL extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", FastGQL.class.getName());
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

    GraphQL graphQL = GraphQLUtils.create(database, client);

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
