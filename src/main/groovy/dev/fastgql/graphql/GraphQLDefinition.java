/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import com.google.inject.assistedinject.Assisted;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.dsl.PermissionsSpec;
import dev.fastgql.events.DebeziumEngineSingleton;
import dev.fastgql.events.EventFlowableFactory;
import dev.fastgql.sql.*;
import dev.fastgql.sql.MutationExecution;
import graphql.GraphQL;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.FieldCoordinates;
import graphql.schema.GraphQLCodeRegistry;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.SelectedField;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.impl.JWTUser;
import io.vertx.ext.web.handler.graphql.VertxDataFetcher;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Transaction;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to build {@link GraphQL} from {@link DatabaseSchema} (used for defining GraphQL schema) and
 * SQL connection pool ({@link Pool} - used in data fetchers).
 *
 * @author Kamil Bobrowski
 */
public class GraphQLDefinition {

  private static final Logger log = LoggerFactory.getLogger(GraphQLDefinition.class);

  public interface BuilderFactory {
    Builder create(DatabaseSchema databaseSchema);
  }

  public interface Builder {
    Builder enableQuery();

    Builder enableSubscription(Vertx vertx, DebeziumConfig debeziumConfig);

    Builder enableMutation();

    GraphQL build();
  }

  public static class DefaultBuilder implements Builder {
    private final DatabaseSchema databaseSchema;
    private final GraphQLDatabaseSchema graphQLDatabaseSchema;
    private final Pool sqlConnectionPool;
    private final GraphQLSchema.Builder graphQLSchemaBuilder;
    private final GraphQLCodeRegistry.Builder graphQLCodeRegistryBuilder;
    private final Function<Transaction, QueryExecutor> transactionQueryExecutorFunction;
    private final DebeziumEngineSingleton debeziumEngineSingleton;
    private final EventFlowableFactory eventFlowableFactory;
    private final Function<Set<TableAlias>, String> lockQueryFunction;
    private final String unlockQuery;
    private final Supplier<PermissionsSpec> permissionsSpecSupplier;
    private boolean queryEnabled = false;
    private boolean mutationEnabled = false;
    private boolean subscriptionEnabled = false;
    private boolean returningStatementEnabled = false;

    /**
     * Class builder, has to be initialized with database schema and SQL connection pool.
     *
     * @param databaseSchema input database schema
     * @param sqlConnectionPool SQL connection pool
     */
    @Inject
    public DefaultBuilder(
        @Assisted DatabaseSchema databaseSchema,
        Pool sqlConnectionPool,
        DatasourceConfig datasourceConfig,
        Function<Transaction, QueryExecutor> transactionQueryExecutorFunction,
        Supplier<PermissionsSpec> permissionsSpecSupplier,
        DebeziumEngineSingleton debeziumEngineSingleton,
        EventFlowableFactory eventFlowableFactory) {
      this.databaseSchema = databaseSchema;
      this.graphQLDatabaseSchema = new GraphQLDatabaseSchema(databaseSchema);
      this.sqlConnectionPool = sqlConnectionPool;
      this.graphQLSchemaBuilder = GraphQLSchema.newSchema();
      this.graphQLCodeRegistryBuilder = GraphQLCodeRegistry.newCodeRegistry();
      if (datasourceConfig.getDbType().equals(DatasourceConfig.DBType.postgresql)) {
        returningStatementEnabled = true;
      }
      this.transactionQueryExecutorFunction = transactionQueryExecutorFunction;
      this.debeziumEngineSingleton = debeziumEngineSingleton;
      this.eventFlowableFactory = eventFlowableFactory;
      this.lockQueryFunction = datasourceConfig.tableListLockQueryFunction();
      this.unlockQuery = datasourceConfig.getUnlockQuery();
      this.permissionsSpecSupplier = permissionsSpecSupplier;
    }

    private Single<Map<String, Object>> getResponseMutation(
        DataFetchingEnvironment env, Transaction transaction) {
      String fieldName = env.getField().getName();
      Object rowsObject = env.getArgument("objects");
      JsonArray rows = rowsObject == null ? null : new JsonArray((List<?>) rowsObject);
      SelectedField returning = env.getSelectionSet().getField("returning");
      List<String> returningColumns = new ArrayList<>();
      if (returning != null) {
        returning
            .getSelectionSet()
            .getFields()
            .forEach(selectedField -> returningColumns.add(selectedField.getName()));
      }
      return MutationExecution.createResponse(
          transaction, databaseSchema, fieldName, rows, returningColumns);
    }

    private ExecutionFunctions createExecutionFunctions(
        DataFetchingEnvironment env, Map<String, Object> userParams) {
      String role = (String) userParams.getOrDefault("role", "default");
      PermissionsSpec permissionsSpec = permissionsSpecSupplier.get();
      System.out.println(permissionsSpec);
      if (permissionsSpec == null) {
        throw new RuntimeException("No permissions defined");
      }
      return new ExecutionFunctions(
          graphQLDatabaseSchema,
          permissionsSpec.getRole(role),
          userParams,
          lockQueryFunction,
          unlockQuery);
    }

    private ExecutionDefinition createExecutionDefinition(
        DataFetchingEnvironment env, Map<String, Object> userParams) {
      Field field = env.getField();
      ExecutionFunctions executionFunctions = createExecutionFunctions(env, userParams);
      return executionFunctions.createExecutionDefinition(field, true);
    }

    private Function<Transaction, Maybe<List<Map<String, Object>>>>
        createTransactionQueryResponseFunction(ExecutionDefinition executionDefinition) {
      return transaction ->
          executionDefinition
              .getQueryExecutorResponseFunction()
              .apply(transactionQueryExecutorFunction.apply(transaction))
              .flatMap(result -> transaction.rxCommit().andThen(Maybe.just(result)));
    }

    private static Stream<Field> createFieldStream(DataFetchingEnvironment env) {
      return env.getDocument().getDefinitions().stream()
          .filter(definition -> definition instanceof OperationDefinition)
          .map(definition -> (OperationDefinition) definition)
          .filter(
              operationDefinition ->
                  operationDefinition.getOperation().equals(OperationDefinition.Operation.QUERY))
          .map(operationDefinition -> operationDefinition.getSelectionSet().getSelections())
          .flatMap(List::stream)
          .filter(selection -> selection instanceof Field)
          .map(selection -> (Field) selection);
    }

    private static Map<String, Object> createUserParamsQuery(DataFetchingEnvironment env) {
      RoutingContext routingContext = env.getContext();
      User user = routingContext.getDelegate().user();
      JWTUser jwtUser = (JWTUser) user;
      return jwtUser == null ? Map.of() : jwtUser.principal().getMap();
    }

    private static Map<String, Object> createUserParamsSubscription(DataFetchingEnvironment env) {
      return Map.of();
    }

    /**
     * Enables query by defining data fetcher using {@link VertxDataFetcher} and adding it to {@link
     * GraphQLCodeRegistry}.
     *
     * @return this
     */
    public Builder enableQuery() {
      if (queryEnabled) {
        return this;
      }
      VertxDataFetcher<List<Map<String, Object>>> queryDataFetcher =
          new VertxDataFetcher<>(
              (env, promise) -> {
                boolean hasMeta =
                    createFieldStream(env).anyMatch(field -> field.getName().equals("__meta"));

                if (hasMeta) {
                  promise.complete(null);
                } else {
                  sqlConnectionPool
                      .rxBegin()
                      .flatMapMaybe(
                          transaction ->
                              createTransactionQueryResponseFunction(
                                      createExecutionDefinition(env, createUserParamsQuery(env)))
                                  .apply(transaction))
                      .defaultIfEmpty(List.of())
                      .subscribe(promise::complete, promise::fail);
                }
              });

      VertxDataFetcher<Map<String, Object>> metaDataFetcher =
          new VertxDataFetcher<>(
              (env, promise) -> {
                Map<String, Object> userParams = createUserParamsQuery(env);
                ExecutionFunctions executionFunctions = createExecutionFunctions(env, userParams);

                Map<String, String> mockQueries =
                    createFieldStream(env)
                        .filter(field -> !field.getName().equals("__meta"))
                        .map(
                            field ->
                                Map.entry(
                                    field.getName(),
                                    String.join("; ", executionFunctions.createMockQueries(field))
                                        .strip()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                promise.complete(Map.of("sql", mockQueries, "user", userParams.toString()));
              });

      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates("Query", tableName), queryDataFetcher));
      graphQLCodeRegistryBuilder.dataFetcher(
          FieldCoordinates.coordinates("Query", "__meta"), metaDataFetcher);
      queryEnabled = true;
      return this;
    }

    /**
     * Enables mutation by defining data fetcher using {@link VertxDataFetcher} and adding it to
     * {@link GraphQLCodeRegistry}.
     *
     * @return this
     */
    public Builder enableMutation() {
      if (mutationEnabled) {
        return this;
      }

      VertxDataFetcher<Map<String, Object>> mutationDataFetcher =
          new VertxDataFetcher<>(
              (env, promise) ->
                  sqlConnectionPool
                      .rxBegin()
                      .flatMap(
                          transaction ->
                              getResponseMutation(env, transaction)
                                  .flatMap(
                                      result ->
                                          transaction.rxCommit().andThen(Single.just(result))))
                      .subscribe(promise::complete, promise::fail));

      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates(
                          "Mutation", String.format("insert_%s", tableName)),
                      mutationDataFetcher));

      mutationEnabled = true;
      return this;
    }

    /**
     * Enables subscription by defining subscription data fetcher and adding it to {@link
     * GraphQLCodeRegistry}.
     *
     * @param vertx vertx instance
     * @param debeziumConfig debezium config
     * @return this
     */
    public Builder enableSubscription(Vertx vertx, DebeziumConfig debeziumConfig) {

      if (subscriptionEnabled || !debeziumConfig.isActive()) {
        log.debug("Subscription already enabled or debezium is not configured");
        return this;
      }

      if (debeziumConfig.isEmbedded()) {
        try {
          debeziumEngineSingleton.startNewEngine();
        } catch (IOException e) {
          log.error("subscription not enabled: debezium engine could not start");
          return this;
        }
      }

      DataFetcher<Flowable<List<Map<String, Object>>>> subscriptionDataFetcher =
          env -> {
            ExecutionDefinition executionDefinition =
                createExecutionDefinition(env, createUserParamsSubscription(env));
            Function<Transaction, Maybe<List<Map<String, Object>>>>
                transactionQueryResponseFunction =
                    createTransactionQueryResponseFunction(executionDefinition);
            return eventFlowableFactory
                .create(executionDefinition.getQueriedTables())
                .flatMapSingle(record -> sqlConnectionPool.rxBegin())
                .flatMapMaybe(transactionQueryResponseFunction::apply)
                .defaultIfEmpty(List.of());
          };

      databaseSchema
          .getTableNames()
          .forEach(
              tableName ->
                  graphQLCodeRegistryBuilder.dataFetcher(
                      FieldCoordinates.coordinates("Subscription", tableName),
                      subscriptionDataFetcher));
      subscriptionEnabled = true;
      return this;
    }

    /**
     * Build {@link GraphQL} by applying internally constructed {@link GraphQLDatabaseSchema} to
     * query / subscription builders.
     *
     * @return constructed GraphQL object
     */
    public GraphQL build() {
      if (!(queryEnabled || subscriptionEnabled)) {
        throw new RuntimeException("query or subscription has to be enabled");
      }
      GraphQLObjectType.Builder queryBuilder = GraphQLObjectType.newObject().name("Query");
      GraphQLObjectType.Builder subscriptionBuilder =
          GraphQLObjectType.newObject().name("Subscription");
      GraphQLObjectType.Builder mutationBuilder = GraphQLObjectType.newObject().name("Mutation");
      List<GraphQLObjectType.Builder> builders = new ArrayList<>();
      if (queryEnabled) {
        builders.add(queryBuilder);
      }
      if (subscriptionEnabled) {
        builders.add(subscriptionBuilder);
      }
      graphQLDatabaseSchema.applyToGraphQLObjectTypes(builders);
      if (queryEnabled) {
        graphQLDatabaseSchema.applyToQueryObjectType(queryBuilder);
        graphQLSchemaBuilder.query(queryBuilder);
      }
      if (subscriptionEnabled) {
        graphQLSchemaBuilder.subscription(subscriptionBuilder);
      }
      if (mutationEnabled) {
        graphQLDatabaseSchema.applyMutation(mutationBuilder, returningStatementEnabled);
        graphQLSchemaBuilder.mutation(mutationBuilder);
      }
      GraphQLSchema graphQLSchema =
          graphQLSchemaBuilder.codeRegistry(graphQLCodeRegistryBuilder.build()).build();
      return GraphQL.newGraphQL(graphQLSchema).build();
    }
  }
}
