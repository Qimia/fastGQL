package dev.fastgql.sql;

import dev.fastgql.common.ReferenceType;
import dev.fastgql.common.RelationalOperator;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.graphql.GraphQLDatabaseSchema;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Field;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.reactivex.sqlclient.Row;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryFunctions {

  private final GraphQLDatabaseSchema graphQLDatabaseSchema;
  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;
  private final Function<Set<TableAlias>, String> tableListLockQueryFunction;
  private final String unlockQuery;
  private final List<String> queriesToExecute;
  private final DatasourceConfig.DBType dbType;
  private final Set<TableAlias> tableAliasesFromArguments = new HashSet<>();
  private final Set<TableAlias> tableAliasesFromQuery = new HashSet<>();

  public QueryFunctions(
      GraphQLDatabaseSchema graphQLDatabaseSchema,
      RoleSpec roleSpec,
      Map<String, Object> jwtParams,
      Function<Set<TableAlias>, String> tableListLockQueryFunction,
      String unlockQuery,
      DatasourceConfig.DBType dbType) {

    this.graphQLDatabaseSchema = graphQLDatabaseSchema;
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
    this.tableListLockQueryFunction = tableListLockQueryFunction;
    this.unlockQuery = unlockQuery;
    this.queriesToExecute = new ArrayList<>();
    this.dbType = dbType;
  }

  private RowExecutor createExecutorForColumn(Table table, GraphQLField graphQLField, Query query) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    if (!table.isColumnAllowed(columnName)) {
      throw new RuntimeException(
          String.format(
              "No permission to access column %s of table %s", columnName, table.getTableName()));
    }
    SelectColumn selectColumn = query.addSelectColumn(table, columnName);
    return (queryExecutor, row) -> {
      Object value = row == null ? null : row.getValue(selectColumn.getResultAlias());
      return value == null ? Maybe.empty() : Maybe.just(Map.entry(columnName, value));
    };
  }

  private RowExecutor createExecutorForReferencing(
      Table table,
      Field field,
      GraphQLField graphQLField,
      Query query,
      Map<String, String> pathInQueryToAlias,
      String pathInQuery) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();

    String newPathInQuery = String.format("%s/%s", pathInQuery, field.getName());

    Table foreignTable =
        new Table(
            foreignTableName,
            pathInQueryToAlias.get(newPathInQuery),
            roleSpec,
            new Arguments(),
            null,
            jwtParams,
            newPathInQuery);
    SelectColumn selectColumnReferencing =
        query.addLeftJoin(table, columnName, foreignTable, foreignColumnName);
    List<RowExecutor> executors =
        createExecutors(foreignTable, field, query, pathInQueryToAlias, newPathInQuery);
    return (queryExecutor, row) -> {
      Object value = row.getValue(selectColumnReferencing.getResultAlias());
      return value == null
          ? Maybe.empty()
          : createResponseForRow(queryExecutor, executors, row)
              .map(result -> Map.entry(field.getName(), result));
    };
  }

  private RowExecutor createExecutorForReferenced(
      Table table,
      Field field,
      GraphQLField graphQLField,
      Query query,
      Map<String, String> pathInQueryToAlias,
      String pathInQuery) {

    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    SelectColumn selectColumn = query.addSelectColumn(table, columnName);

    String newPathInQuery = String.format("%s/%s", pathInQuery, field.getName());

    Condition placeholderExtraCondition =
        new Condition(foreignColumnName, RelationalOperator._eq, jwtParams -> new Object());

    BiFunction<QueryExecutor, Condition, Maybe<List<Map<String, Object>>>> conditionSingleFunction =
        queryExecutorConditionResponseFunction(
            foreignTableName, field, placeholderExtraCondition, pathInQueryToAlias, newPathInQuery);

    return (queryExecutor, row) -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null
          ? Maybe.empty()
          : conditionSingleFunction
              .apply(queryExecutor, ConditionUtils.checkColumnIsEqValue(foreignColumnName, value))
              .map(result -> Map.entry(field.getName(), result));
    };
  }

  private List<RowExecutor> createExecutors(
      Table table,
      Field field,
      Query query,
      Map<String, String> pathInQueryToAlias,
      String pathInQuery) {
    AtomicInteger count = new AtomicInteger();
    return field.getSelectionSet().getSelections().stream()
        .filter(selection -> selection instanceof Field)
        .map(selection -> (Field) selection)
        .map(
            subField -> {
              GraphQLField graphQLField =
                  graphQLDatabaseSchema.fieldAt(table.getTableName(), subField.getName());
              switch (graphQLField.getReferenceType()) {
                case NONE:
                  return createExecutorForColumn(table, graphQLField, query);
                case REFERENCING:
                  return createExecutorForReferencing(
                      table, subField, graphQLField, query, pathInQueryToAlias, pathInQuery);
                case REFERENCED:
                  return createExecutorForReferenced(
                      table, subField, graphQLField, query, pathInQueryToAlias, pathInQuery);
                default:
                  throw new RuntimeException(
                      "Unknown reference type: " + graphQLField.getReferenceType());
              }
            })
        .collect(Collectors.toList());
  }

  private Map<String, TableAlias> createPathInQueryToTableAlias(
      String currentPathInQuery, String currentAlias, String tableName, Field field) {
    Map<String, TableAlias> pathInQueryToAlias = new HashMap<>();
    String newPathInQuery =
        currentPathInQuery != null
            ? String.format("%s/%s", currentPathInQuery, field.getName())
            : field.getName();
    pathInQueryToAlias.put(newPathInQuery, new TableAlias(tableName, currentAlias));

    AtomicInteger count = new AtomicInteger();

    return field.getSelectionSet().getSelections().stream()
        .filter(selection -> selection instanceof Field)
        .map(selection -> (Field) selection)
        .filter(subField -> !subField.getName().equals("__sql"))
        .map(
            subField -> {
              GraphQLField graphQLField =
                  graphQLDatabaseSchema.fieldAt(tableName, subField.getName());
              return graphQLField.getReferenceType() == ReferenceType.REFERENCING
                      || graphQLField.getReferenceType() == ReferenceType.REFERENCED
                  ? createPathInQueryToTableAlias(
                      newPathInQuery,
                      String.format("%s_%d", currentAlias, count.getAndIncrement()),
                      graphQLField.getForeignName().getTableName(),
                      subField)
                  : new HashMap<String, TableAlias>();
            })
        .reduce(
            pathInQueryToAlias,
            (accumulated, current) -> {
              accumulated.putAll(current);
              return accumulated;
            })
        .entrySet()
        .stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Maybe<Map<String, Object>> createResponseForRow(
      QueryExecutor queryExecutor, List<RowExecutor> executorList, Row row) {
    return Observable.fromIterable(executorList)
        .flatMapMaybe(rowExecutor -> rowExecutor.apply(queryExecutor, row))
        .collectInto(
            (Map<String, Object>) new HashMap<String, Object>(),
            (accumulator, newEntry) -> accumulator.put(newEntry.getKey(), newEntry.getValue()))
        .filter(map -> !map.isEmpty());
  }

  private BiFunction<QueryExecutor, Condition, Maybe<List<Map<String, Object>>>>
      queryExecutorConditionResponseFunction(
          String tableName,
          Field field,
          Condition extraConditionPlaceholder,
          Map<String, String> pathInQueryToAlias,
          String pathInQuery) {

    String tableAlias = pathInQueryToAlias.get(pathInQuery);

    Arguments arguments =
        new Arguments(field.getArguments(), tableName, tableAlias, graphQLDatabaseSchema);
    // Condition condition = arguments.getCondition();
    // List<OrderBy> orderByList = arguments.getOrderByList();
    if (arguments.getCondition() != null) {
      tableAliasesFromArguments.addAll(
          ConditionUtils.conditionToTableAliasSet(arguments.getCondition(), tableAlias));
    }

    if (arguments.getOrderByList() != null) {
      tableAliasesFromArguments.addAll(
          OrderByUtils.orderByListToTableAliasSet(arguments.getOrderByList()));
    }

    Table table =
        new Table(
            tableName,
            tableAlias,
            roleSpec,
            arguments,
            extraConditionPlaceholder,
            jwtParams,
            pathInQuery);

    Query query = new Query(table);
    List<RowExecutor> executorList =
        createExecutors(query.getTable(), field, query, pathInQueryToAlias, pathInQuery);

    String queryString = query.buildQuery(dbType);
    queriesToExecute.add(queryString);

    return (queryExecutor, extraCondition) -> {
      table.setExtraCondition(extraCondition);
      List<Object> params = query.buildParams();
      return queryExecutor
          .apply(queryString, params)
          .flatMapObservable(Observable::fromIterable)
          .flatMapMaybe(row -> createResponseForRow(queryExecutor, executorList, row))
          .toList()
          .filter(list -> !list.isEmpty());
    };
  }

  public List<String> createQueriesToExecute(Field field) {
    Map<String, TableAlias> pathInQueryToTableAlias = createPathInQueryToTableAlias(field);
    Map<String, String> pathInQueryToAlias = createPathInQueryToAlias(pathInQueryToTableAlias);
    Set<TableAlias> tableAliases = createTableAliases(pathInQueryToTableAlias);

    String tableLockQueryString = tableListLockQueryFunction.apply(tableAliases);

    queriesToExecute.clear();

    queryExecutorConditionResponseFunction(
        field.getName(), field, null, pathInQueryToAlias, field.getName());

    List<String> mockQueriesReversed =
        Stream.of(
                List.of(unlockQuery == null ? "" : unlockQuery),
                queriesToExecute,
                List.of(tableLockQueryString))
            .flatMap(List::stream)
            .filter(query -> !query.isEmpty())
            .collect(Collectors.toList());
    Collections.reverse(mockQueriesReversed);
    return mockQueriesReversed;
  }

  private Map<String, TableAlias> createPathInQueryToTableAlias(Field field) {
    return createPathInQueryToTableAlias(null, "t", field.getName(), field);
  }

  private Map<String, String> createPathInQueryToAlias(
      Map<String, TableAlias> pathInQueryToTableAlias) {
    return pathInQueryToTableAlias.entrySet().stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> entry.getValue().getTableAlias()));
  }

  private Set<TableAlias> createTableAliases(Map<String, TableAlias> pathInQueryToTableAlias) {
    return new HashSet<>(pathInQueryToTableAlias.values());
  }

  public ExecutionDefinition<List<Map<String, Object>>> createExecutionDefinition(
      Field field, boolean lockTables) {

    Map<String, TableAlias> pathInQueryToTableAlias = createPathInQueryToTableAlias(field);
    Map<String, String> pathInQueryToAlias = createPathInQueryToAlias(pathInQueryToTableAlias);
    Set<TableAlias> tableAliases = createTableAliases(pathInQueryToTableAlias);

    queriesToExecute.clear();
    tableAliasesFromArguments.clear();

    BiFunction<QueryExecutor, Condition, Maybe<List<Map<String, Object>>>>
        queryResultSingleFunction =
            queryExecutorConditionResponseFunction(
                field.getName(), field, null, pathInQueryToAlias, field.getName());

    tableAliases.addAll(tableAliasesFromArguments);

    String tableLockQueryString = tableListLockQueryFunction.apply(tableAliases);
    String tableUnlockQueryString = unlockQuery;

    Function<QueryExecutor, Maybe<List<Map<String, Object>>>> queryExecutorResponseFunction =
        queryExecutor -> {
          Completable tableLockCompletable =
              queryExecutor.apply(tableLockQueryString).ignoreElement();
          Completable tableUnlockCompletable =
              tableUnlockQueryString == null
                  ? Completable.complete()
                  : queryExecutor.apply(tableUnlockQueryString).ignoreElement();

          Maybe<List<Map<String, Object>>> queryResultSingle =
              queryResultSingleFunction.apply(queryExecutor, null);

          return tableLockQueryString == null || !lockTables
              ? queryResultSingle
              : tableLockCompletable
                  .andThen(queryResultSingle)
                  .flatMap(result -> tableUnlockCompletable.andThen(Maybe.just(result)));
        };

    Set<String> queriedTables =
        pathInQueryToTableAlias.values().stream()
            .map(TableAlias::getTableName)
            .collect(Collectors.toSet());

    return new ExecutionDefinition<>(queryExecutorResponseFunction, queriedTables);
  }
}
