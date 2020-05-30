/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package ai.qimia.fastgql;

import static graphql.Scalars.GraphQLInt;

import com.google.common.collect.Iterables;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLSchema;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphQLHandler;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerConfig;

@SuppressWarnings({"rawtypes", "ResultOfMethodCallIgnored"})
public class GraphQLServer extends AbstractVerticle {

  private static final String alteredTablesTopic = "altered-tables";
  private static KafkaConsumer<String, String> kafkaConsumer;

  public static void main(String[] args) {
    Launcher.executeCommand("run", GraphQLServer.class.getName());
  }

  private static String kafkaMessageToTableName(String message) {
    return Iterables.getLast(
        Arrays.asList(message.substring(1, message.length() - 1).split("\\."))
    );
  }

  private static Map<String, TableSchema<?>> fetchTableSchemas(DatasourceConfig datasourceConfig)
      throws SQLException {
    Map<String, TableSchema<?>> tableSchemas;
    try (
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
    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        config().getString("bootstrap.servers", "http://localhost:9092"));
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    config.put(ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID());
    config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return KafkaConsumer.create(vertx, config);
  }


  private GraphQL buildGraphQL(Map<String, TableSchema<?>> tableSchemas,
      DatasourceConfig datasourceConfig) {
    GraphQLObjectType.Builder queryType = GraphQLObjectType.newObject()
        .name("Query");

    GraphQLObjectType.Builder subscriptionType = GraphQLObjectType.newObject()
        .name("Subscription");

    tableSchemas.forEach(
        (tableName, tableSchema) -> {
          GraphQLOutputType outputType = tableSchema.graphQLOutputType(tableSchemas);
          GraphQLInputType orderByType = tableSchema.orderByType();
          GraphQLInputType distinctOnType = tableSchema.selectColumnType();
          GraphQLInputType whereType = tableSchema.boolExpType();

          GraphQLArgument limit = GraphQLArgument.newArgument().name("limit").type(GraphQLInt)
              .build();
          GraphQLArgument offset = GraphQLArgument.newArgument().name("offset").type(GraphQLInt)
              .build();
          GraphQLArgument orderBy = GraphQLArgument.newArgument().name("order_by").type(orderByType)
              .build();
          GraphQLArgument distinctOn = GraphQLArgument.newArgument().name("distinct_on")
              .type(distinctOnType).build();
          GraphQLArgument where = GraphQLArgument.newArgument().name("where").type(whereType)
              .build();

          queryType.field(GraphQLFieldDefinition.newFieldDefinition()
              .name(tableName)
              .type(outputType)
              .argument(limit)
              .argument(offset)
              .argument(orderBy)
              .argument(distinctOn)
              .argument(where)
              .build());
          subscriptionType.field(GraphQLFieldDefinition.newFieldDefinition()
              .name(tableName)
              .type(outputType)
              .argument(limit)
              .argument(offset)
              .argument(orderBy)
              .argument(distinctOn)
              .argument(where)
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
                    (environment, fetcherPromise) -> JDBCUtils
                        .getGraphQLResponse(environment, client)
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
                    .filter(record -> kafkaMessageToTableName(record.value()).equals(key))
                    .flatMap(
                        record -> JDBCUtils.getGraphQLResponse(environment, client).toFlowable())
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

    // start kafka consumer
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
    ApolloWSHandlerUpdatable apolloWSHandler = ApolloWSHandlerUpdatable.create();
    apolloWSHandler.updateGraphQL(graphQL);

    GraphQLHandlerUpdatable graphQLHandler = GraphQLHandlerUpdatable.create();
    graphQLHandler.updateGraphQL(graphQL);

    // set up router
    Router router = Router.router(vertx);
    router.route("/graphql").handler(apolloWSHandler);
    router.route("/graphql").handler(graphQLHandler);
    router.route("/graphiql/*").handler(GraphiQLHandler.create(
      new GraphiQLHandlerOptions().setEnabled(true)
    ));

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
