package ai.qimia.fastgql;

import ai.qimia.fastgql.common.FieldType;
import ai.qimia.fastgql.db.DatabaseSchema;
import ai.qimia.fastgql.graphql.GraphQLUtils;
import ai.qimia.fastgql.router.RouterUpdatable;
import graphql.GraphQL;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.SqlConnection;
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
    RouterUpdatable routerUpdatable = new RouterUpdatable(vertx);
    routerUpdatable.update(graphQL);

    vertx
      .createHttpServer()
      .requestHandler(routerUpdatable.getRouter())
      .rxListen(config().getInteger("http.port", 8080))
      .subscribe(server -> future.complete(), server -> future.fail(server.getCause()));
  }
}
