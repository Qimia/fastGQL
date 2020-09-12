package dev.fastgql.newsql;

import dev.fastgql.common.ReferenceType;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Field;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ExecutionFunctions {

  private static RowExecutor createExecutorForColumn(
      Table table, GraphQLField graphQLField, Query query) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    if (!table.isColumnAllowed(columnName)) {
      throw new RuntimeException(
          String.format(
              "No permission to access column %s of table %s", columnName, table.getTableName()));
    }
    Query.SelectColumn selectColumn = query.addSelectColumn(table, columnName);
    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null ? Single.just(Map.of()) : Single.just(Map.of(columnName, value));
    };
  }

  private static RowExecutor createExecutorForReferencing(
      Table table,
      Field field,
      GraphQLField graphQLField,
      Query query,
      ExecutionConstants executionConstants,
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
            executionConstants.getRoleSpec(),
            new Arguments(),
            null,
            executionConstants.getJwtParams(),
            pathInQueryToAlias);
    Query.SelectColumn selectColumnReferencing =
        query.addLeftJoin(table, columnName, foreignTable, foreignColumnName);
    List<RowExecutor> executors =
        createExecutors(
            foreignTable, field, query, executionConstants, pathInQueryToAlias, newPathInQuery);
    return row -> {
      Object value = row.getValue(selectColumnReferencing.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : executorListToSingleMap(executors, row).map(result -> Map.of(field.getName(), result));
    };
  }

  private static RowExecutor createExecutorForReferenced(
      Table table,
      Field field,
      GraphQLField graphQLField,
      Query query,
      ExecutionConstants executionConstants,
      Map<String, String> pathInQueryToAlias,
      String pathInQuery
      ) {

    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    Query.SelectColumn selectColumn = query.addSelectColumn(table, columnName);

    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : getRootResponseInternal(
                  foreignTableName,
                  field,
                  executionConstants,
                  ConditionUtils.checkColumnIsEqValue(foreignColumnName, value),
                  pathInQueryToAlias,
                  pathInQuery
              )
              .map(result -> Map.of(field.getName(), result));
    };
  }

  private static List<RowExecutor> createExecutors(
      Table table,
      Field field,
      Query query,
      ExecutionConstants executionConstants,
      Map<String, String> pathInQueryToAlias,
      String pathInQuery) {
    return field.getSelectionSet().getSelections().stream()
        .filter(selection -> selection instanceof Field)
        .map(selection -> (Field) selection)
        .map(
            subField -> {
              GraphQLField graphQLField =
                  executionConstants
                      .getGraphQLDatabaseSchema()
                      .fieldAt(table.getTableName(), subField.getName());
              switch (graphQLField.getReferenceType()) {
                case NONE:
                  return createExecutorForColumn(table, graphQLField, query);
                case REFERENCING:
                  return createExecutorForReferencing(
                      table,
                      subField,
                      graphQLField,
                      query,
                      executionConstants,
                      pathInQueryToAlias,
                      pathInQuery);
                case REFERENCED:
                  return createExecutorForReferenced(
                      table, subField, graphQLField, query, executionConstants, pathInQueryToAlias, pathInQuery);
                default:
                  throw new RuntimeException(
                      "Unknown reference type: " + graphQLField.getReferenceType());
              }
            })
        .collect(Collectors.toList());
  }

  private static Map<String, TableAlias> createPathInQueryToTableAlias(
    String currentPathInQuery,
    String currentAlias,
    String tableName,
    Field field,
    ExecutionConstants executionConstants) {
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
              executionConstants
                .getGraphQLDatabaseSchema()
                .fieldAt(tableName, subField.getName());
            return graphQLField.getReferenceType() == ReferenceType.REFERENCING || graphQLField.getReferenceType() == ReferenceType.REFERENCED
              ? createPathInQueryToTableAlias(
              newPathInQuery,
              String.format("%s_%d", currentAlias, count.getAndIncrement()),
              graphQLField.getForeignName().getTableName(),
              subField,
              executionConstants)
              : new HashMap<String, TableAlias>();
          })
        .reduce(
          pathInQueryToAlias,
          (accumulated, current) -> {
            accumulated.putAll(current);
            return accumulated;
          });

    }

  public static Single<Map<String, Object>> executorListToSingleMap(
      List<RowExecutor> executorList, Row row) {
    return Observable.fromIterable(executorList)
        .flatMapSingle(executor -> executor.apply(row))
        .collect(HashMap::new, Map::putAll);
  }

  public static Single<List<Map<String, Object>>> getRootResponseInternal(
    String tableName,
    Field field,
    ExecutionConstants executionConstants,
    Condition extraCondition,
    Map<String, String> pathInQueryToAlias,
    String pathInQuery
  ) {
    Table table =
      new Table(
        tableName,
        pathInQueryToAlias.get(pathInQuery),
        executionConstants.getRoleSpec(),
        new Arguments(field.getArguments(), pathInQuery),
        extraCondition,
        executionConstants.getJwtParams(),
        pathInQueryToAlias);

    Query query = new Query(table);
    List<RowExecutor> executorList =
      createExecutors(
        query.getTable(), field, query, executionConstants, pathInQueryToAlias, pathInQuery);

    String queryString = query.createQuery();

    return executionConstants
      .getQueryExecutor()
      .apply(queryString, executorList, ExecutionFunctions::executorListToSingleMap)
      .toList();

  }

  public static Single<List<Map<String, Object>>> getRootResponse(
      String tableName,
      Field field,
      ExecutionConstants executionConstants,
      Condition extraCondition,
      boolean lockTables
      ) {
    Map<String, TableAlias> pathInQueryToTableAlias =
        createPathInQueryToTableAlias(null, "t", tableName, field, executionConstants);

    Map<String, String> pathInQueryToAlias = pathInQueryToTableAlias.entrySet().stream().collect(Collectors.toMap(
      Map.Entry::getKey, entry -> entry.getValue().getTableAlias()
    ));

    Set<TableAlias> tableAliases = new HashSet<>(pathInQueryToTableAlias.values());

    String tableLockQueryString = executionConstants.getTableListLockQueryFunction().apply(tableAliases);
    String tableUnlockQueryString = executionConstants.getUnlockQuery();

    QueryExecutor queryExecutor = executionConstants.getQueryExecutor();
    Observable<Map<String, Object>> tableLockObservable = queryExecutor.justQuery(tableLockQueryString);
    Observable<Map<String, Object>> tableUnlockObservable = tableUnlockQueryString == null ? Observable.just(Map.of())
      : queryExecutor.justQuery(tableUnlockQueryString);

    Single<List<Map<String, Object>>> queryResultSingle = getRootResponseInternal(
      tableName,
      field,
      executionConstants,
      extraCondition,
      pathInQueryToAlias,
      tableName
    );

    return tableLockQueryString == null || !lockTables ? queryResultSingle : tableLockObservable.toList()
      .flatMap(lockResult -> queryResultSingle)
      .flatMap(result -> tableUnlockObservable.toList().map(unlockResult -> result));
  }
}
