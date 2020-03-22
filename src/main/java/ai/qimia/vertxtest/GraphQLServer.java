package ai.qimia.vertxtest;

import com.google.common.collect.Iterables;
import graphql.GraphQL;
import graphql.schema.*;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"rawtypes", "ResultOfMethodCallIgnored"})
public class GraphQLServer extends AbstractVerticle {

  private static final String alteredTablesTopic = "altered-tables";
  private static KafkaConsumer<String, String> kafkaConsumer;

  public static void main(String[] args) {
    Launcher.executeCommand("run", GraphQLServer.class.getName());
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

  private KafkaConsumer<String, String> buildKafkaConsumer() {
    Map<String, String> config = new HashMap<>();
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config().getString("bootstrap.servers", "http://localhost:9092"));
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID());
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return KafkaConsumer.create(vertx, config);
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

    Map<String, DataFetcher> queryDataFetchers = tableSchemas
      .keySet()
      .stream()
      .collect(
        Collectors.toMap(
          Function.identity(),
          key -> new VertxDataFetcher<List<Map<String, Object>>>(
            (environment, fetcherPromise) -> JDBCUtils.getGraphQLResponse(environment, client)
              .subscribe(fetcherPromise::complete)
          )
        )
      );

    Map<String, DataFetcher> subscriptionDataFetchers = tableSchemas
      .keySet()
      .stream()
      .collect(
        Collectors.toMap(
          Function.identity(),
          key -> environment -> kafkaConsumer
            .toFlowable()
            .filter(record -> Iterables
              .getLast(
                Arrays.asList(
                  record
                    .value()
                    .substring(1, record.value().length()-1)
                    .split("\\.")
                )
              )
              .equals(key))
            .flatMap(record -> JDBCUtils.getGraphQLResponse(environment, client).toFlowable())
        )
      );

    GraphQLCodeRegistry codeRegistry = GraphQLCodeRegistry.newCodeRegistry()
      .dataFetchers("Query", queryDataFetchers)
      .dataFetchers("Subscription", subscriptionDataFetchers)
      .build();

    GraphQLSchema graphQLSchema = GraphQLSchema.newSchema()
      .query(queryType)
      .subscription(subscriptionType)
      .codeRegistry(codeRegistry)
      .build();

    return GraphQL.newGraphQL(graphQLSchema).build();
  }

  @Override
  public void start(Promise<Void> future) {

    kafkaConsumer = buildKafkaConsumer();
    kafkaConsumer.subscribe(alteredTablesTopic);

    // get datasource configuration
    DatasourceConfig datasourceConfig = new DatasourceConfig();
    if (config().containsKey("datasource")) {
      datasourceConfig = config().getJsonObject("datasource").mapTo(DatasourceConfig.class);
    }

    // fetch table schemas based on database metadata
    Map<String, TableSchema<?>> tableSchemas;
    try {
      tableSchemas = fetchTableSchemas(datasourceConfig);
    } catch (SQLException e) {
      future.fail(e);
      return;
    }

    // construct graphql
    GraphQL graphQL = buildGraphQL(tableSchemas, datasourceConfig);

    // set up router
    Router router = Router.router(vertx);
    router.route("/graphql").handler(ApolloWSHandler.create(graphQL));
    router.route("/graphql").handler(GraphQLHandler.create(graphQL));

    // start server
    vertx
      .createHttpServer(new HttpServerOptions().setWebsocketSubProtocols("graphql-ws"))
      .requestHandler(router)
      .rxListen(config().getInteger("http.port", 8080))
      .subscribe(server -> future.complete(), server -> future.fail(server.getCause()));
  }

  @Override
  public void stop(Promise<Void> future) {
    kafkaConsumer.unsubscribe(future);
  }
}
