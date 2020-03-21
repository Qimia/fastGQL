package ai.qimia.vertxtest;

import graphql.GraphQL;
import graphql.schema.*;
import io.reactivex.Flowable;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.reactivestreams.Publisher;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GraphQLServer extends AbstractVerticle {

  @SuppressWarnings("rawtypes")
  private static Map<String, DataFetcher> getDataFetcherMap(Set<String> keys, DataFetcher dataFetcher) {
    return keys
      .stream()
      .collect(
        Collectors.toMap(
          Function.identity(),
          key -> dataFetcher
        )
      );
  }

  private static Map<String, TableSchema<?>> fetchTableSchemas(DatasourceConfig datasourceConfig) throws SQLException {
    Map<String, TableSchema<?>> tableSchemas;
    try(
      Connection connection = DriverManager.getConnection(
        String.format(
          "jdbc:postgresql://%s:%d/%s",
          datasourceConfig.getHost(),
          datasourceConfig.getPort(),
          datasourceConfig.getDb()
        ),
        datasourceConfig.getUsername(),
        datasourceConfig.getPassword()
      )
    ) {
      tableSchemas = JDBCUtils.getTableSchemas(connection);
    }
    return tableSchemas;
  }

  private GraphQL buildGraphQL(Map<String, TableSchema<?>> tableSchemas, DatasourceConfig datasourceConfig) {
    GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject()
      .name("Query");

    GraphQLObjectType.Builder subscriptionType = GraphQLObjectType.newObject()
      .name("Subscription");

    tableSchemas.forEach(
      (tableName, tableSchema) -> {
        queryType.field(GraphQLFieldDefinition.newFieldDefinition()
          .name(tableName)
          .type(tableSchema.graphQLOutputType("query_", tableSchemas))
          .build());
        subscriptionType.field(GraphQLFieldDefinition.newFieldDefinition()
          .name(tableName)
          .type(tableSchema.graphQLOutputType("subscription_", tableSchemas))
          .build());
      }
    );

    PgPool client = PgPool.pool(
      vertx,
      new PgConnectOptions()
        .setHost(datasourceConfig.getHost())
        .setPort(datasourceConfig.getPort())
        .setDatabase(datasourceConfig.getDb())
        .setUser(datasourceConfig.getUsername())
        .setPassword(datasourceConfig.getPassword()),
      new PoolOptions().setMaxSize(5)
    );

    VertxDataFetcher<List<Map<String, Object>>> dataFetcher = new VertxDataFetcher<>(
      (environment, fetcherPromise) -> {
        //noinspection ResultOfMethodCallIgnored
        JDBCUtils.getGraphQLResponse(environment, client)
          .subscribe(fetcherPromise::complete);
      }
    );

    DataFetcher<Publisher<List<Map<String, Object>>>> rxDataFetcher = environment -> Flowable.combineLatest(
      vertx.periodicStream(2000).toFlowable(),
      JDBCUtils.getGraphQLResponse(environment, client).toFlowable(),
      (tick, response) -> response
    );

    @SuppressWarnings("rawtypes") Map<String, DataFetcher> dataFetchers = getDataFetcherMap(tableSchemas.keySet(), dataFetcher);
    @SuppressWarnings("rawtypes") Map<String, DataFetcher> rxDataFetchers = getDataFetcherMap(tableSchemas.keySet(), rxDataFetcher);

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetchers("Query", dataFetchers)
      .dataFetchers("Subscription", rxDataFetchers)
      .build();

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(queryType)
      .subscription(subscriptionType)
      .codeRegistry(codeRegistry)
      .build();

    return GraphQL.newGraphQL(graphQLSchema).build();
  }

  @Override
  public void start(Promise<Void> promise) {

    // get datasource configuration
    DatasourceConfig datasourceConfig = config().getJsonObject("datasource").mapTo(DatasourceConfig.class);

    // fetch table schemas based on database metadata
    Map<String, TableSchema<?>> tableSchemas;
    try {
      tableSchemas = fetchTableSchemas(datasourceConfig);
    } catch (SQLException e) {
      promise.fail(e);
      return;
    }

    // construct graphql
    GraphQL graphQL = buildGraphQL(tableSchemas, datasourceConfig);

    // set up router
    Router router = Router.router(vertx);
    router.route("/graphql").handler(ApolloWSHandler.create(graphQL));
    router.route("/graphql").handler(GraphQLHandler.create(graphQL));

    // start server
    // noinspection ResultOfMethodCallIgnored
    vertx
      .createHttpServer(new HttpServerOptions().setWebsocketSubProtocols("graphql-ws"))
      .requestHandler(router)
      .rxListen(config().getInteger("http.port", 8080))
      .subscribe(server -> promise.complete(), server -> promise.fail(server.getCause()));
  }
}
