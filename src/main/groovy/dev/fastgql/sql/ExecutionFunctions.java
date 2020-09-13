package dev.fastgql.sql;

import dev.fastgql.common.ReferenceType;
import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.graphql.GraphQLDatabaseSchema;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Field;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionFunctions {

  private final GraphQLDatabaseSchema graphQLDatabaseSchema;
  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;
  private final Function<Set<TableAlias>, String> tableListLockQueryFunction;
  private final String unlockQuery;

  public ExecutionFunctions(
      GraphQLDatabaseSchema graphQLDatabaseSchema,
      RoleSpec roleSpec,
      Map<String, Object> jwtParams,
      Function<Set<TableAlias>, String> tableListLockQueryFunction,
      String unlockQuery) {

    this.graphQLDatabaseSchema = graphQLDatabaseSchema;
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
    this.tableListLockQueryFunction = tableListLockQueryFunction;
    this.unlockQuery = unlockQuery;
  }

  private RowExecutor createExecutorForColumn(Table table, GraphQLField graphQLField, Query query) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    if (!table.isColumnAllowed(columnName)) {
      throw new RuntimeException(
          String.format(
              "No permission to access column %s of table %s", columnName, table.getTableName()));
    }
    Query.SelectColumn selectColumn = query.addSelectColumn(table, columnName);
    return (queryExecutor, row) -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null ? Single.just(Map.of()) : Single.just(Map.of(columnName, value));
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
            "",
            jwtParams,
            newPathInQuery,
            pathInQueryToAlias);
    Query.SelectColumn selectColumnReferencing =
        query.addLeftJoin(table, columnName, foreignTable, foreignColumnName);
    List<RowExecutor> executors =
        createExecutors(foreignTable, field, query, pathInQueryToAlias, newPathInQuery);
    return (queryExecutor, row) -> {
      Object value = row.getValue(selectColumnReferencing.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : executorListReducerFunction(executors, row)
              .apply(queryExecutor)
              .map(result -> Map.of(field.getName(), result));
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
    Query.SelectColumn selectColumn = query.addSelectColumn(table, columnName);

    BiFunction<QueryExecutor, Condition, Single<List<Map<String, Object>>>>
        conditionSingleFunction =
            queryExecutorConditionResponseFunction(
                foreignTableName,
                field,
                String.format(
                    "%s.%s={%s/%s}",
                    pathInQueryToAlias.get(pathInQuery),
                    foreignColumnName,
                    table.getPathInQuery(),
                    columnName),
                pathInQueryToAlias,
                pathInQuery);

    return (queryExecutor, row) -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : conditionSingleFunction
              .apply(queryExecutor, ConditionUtils.checkColumnIsEqValue(foreignColumnName, value))
              .map(result -> Map.of(field.getName(), result));
    };
  }

  private List<RowExecutor> createExecutors(
      Table table,
      Field field,
      Query query,
      Map<String, String> pathInQueryToAlias,
      String pathInQuery) {
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
            });
  }

  private Function<QueryExecutor, Single<Map<String, Object>>> executorListReducerFunction(
      List<RowExecutor> executorList, Row row) {
    return queryExecutor ->
        Observable.fromIterable(executorList)
            .flatMapSingle(executor -> executor.apply(queryExecutor, row))
            .collect(HashMap::new, Map::putAll);
  }

  private BiFunction<QueryExecutor, Condition, Single<List<Map<String, Object>>>>
      queryExecutorConditionResponseFunction(
          String tableName,
          Field field,
          String mockExtraCondition,
          Map<String, String> pathInQueryToAlias,
          String pathInQuery) {
    Table table =
        new Table(
            tableName,
            pathInQueryToAlias.get(pathInQuery),
            roleSpec,
            new Arguments(field.getArguments(), pathInQuery),
            mockExtraCondition,
            jwtParams,
            pathInQuery,
            pathInQueryToAlias);

    Query query = new Query(table);
    List<RowExecutor> executorList =
        createExecutors(query.getTable(), field, query, pathInQueryToAlias, pathInQuery);
    System.out.println("*********** NEW QUERY");
    System.out.println(query.createMockQuery());

    return (queryExecutor, condition) -> {
      table.setExtraCondition(condition);
      String queryString = query.createQuery();
      return queryExecutor
          .apply(
              queryString,
              executorList,
              (executorListInComposer, row) ->
                  executorListReducerFunction(executorListInComposer, row).apply(queryExecutor))
          .toList();
    };
  }

  public ExecutionDefinition createExecutionDefinition(
      String tableName, Field field, boolean lockTables) {
    Map<String, TableAlias> pathInQueryToTableAlias =
        createPathInQueryToTableAlias(null, "t", tableName, field);

    Map<String, String> pathInQueryToAlias =
        pathInQueryToTableAlias.entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getTableAlias()));

    Set<TableAlias> tableAliases = new HashSet<>(pathInQueryToTableAlias.values());

    String tableLockQueryString = tableListLockQueryFunction.apply(tableAliases);
    String tableUnlockQueryString = unlockQuery;

    BiFunction<QueryExecutor, Condition, Single<List<Map<String, Object>>>>
        queryResultSingleFunction =
            queryExecutorConditionResponseFunction(
                tableName, field, "", pathInQueryToAlias, tableName);

    Function<QueryExecutor, Single<List<Map<String, Object>>>> queryExecutorResponseFunction =
        queryExecutor -> {
          Observable<Map<String, Object>> tableLockObservable =
              queryExecutor.justQuery(tableLockQueryString);
          Observable<Map<String, Object>> tableUnlockObservable =
              tableUnlockQueryString == null
                  ? Observable.just(Map.of())
                  : queryExecutor.justQuery(tableUnlockQueryString);

          Single<List<Map<String, Object>>> queryResultSingle =
              queryResultSingleFunction.apply(queryExecutor, null);

          return tableLockQueryString == null || !lockTables
              ? queryResultSingle
              : tableLockObservable
                  .toList()
                  .flatMap(lockResult -> queryResultSingle)
                  .flatMap(result -> tableUnlockObservable.toList().map(unlockResult -> result));
        };

    Set<String> queriedTables =
        pathInQueryToTableAlias.values().stream()
            .map(TableAlias::getTableName)
            .collect(Collectors.toSet());

    return new ExecutionDefinition(queryExecutorResponseFunction, queriedTables);
  }
}
