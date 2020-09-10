package dev.fastgql.newsql;

import dev.fastgql.common.ReferenceType;
import dev.fastgql.dsl.OpSpec;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Field;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionFunctions {

  private static Function<Row, Single<Map<String, Object>>> createExecutorForColumn(
      Table table, GraphQLField graphQLField, Query query) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    Query.SelectColumn selectColumn = query.addSelectColumn(table, columnName);
    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null ? Single.just(Map.of()) : Single.just(Map.of(columnName, value));
    };
  }

  private static Function<Row, Single<Map<String, Object>>> createExecutorForReferencing(
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
            null,
            null,
            executionConstants.getJwtParams(),
            pathInQueryToAlias);
    Query.SelectColumn selectColumnReferencing =
        query.addLeftJoin(table, columnName, foreignTable, foreignColumnName);
    List<Function<Row, Single<Map<String, Object>>>> executors =
        createExecutors(
            foreignTable, field, query, executionConstants, pathInQueryToAlias, newPathInQuery);
    return row -> {
      Object value = row.getValue(selectColumnReferencing.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : executorListToSingleMap(executors, row).map(result -> Map.of(field.getName(), result));
    };
  }

  private static Function<Row, Single<Map<String, Object>>> createExecutorForReferenced(
      Table table,
      Field field,
      GraphQLField graphQLField,
      Query query,
      ExecutionConstants executionConstants) {

    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    Query.SelectColumn selectColumn = query.addSelectColumn(table, columnName);

    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : getRootResponse(
                  foreignTableName,
                  field,
                  executionConstants,
                  OpSpecUtils.checkColumnIsEqValue(foreignColumnName, value))
              .toList()
              .map(result -> Map.of(field.getName(), result));
    };
  }

  private static List<Function<Row, Single<Map<String, Object>>>> createExecutors(
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
                      table, subField, graphQLField, query, executionConstants);
                default:
                  throw new RuntimeException(
                      "Unknown reference type: " + graphQLField.getReferenceType());
              }
            })
        .collect(Collectors.toList());
  }

  private static Map<String, String> createPathInQueryToAlias(
      String currentPathInQuery,
      String currentAlias,
      String tableName,
      Field field,
      ExecutionConstants executionConstants) {
    Map<String, String> pathInQueryToAlias = new HashMap<>();
    String newPathInQuery =
        currentPathInQuery != null
            ? String.format("%s/%s", currentPathInQuery, field.getName())
            : field.getName();
    pathInQueryToAlias.put(newPathInQuery, currentAlias);

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
              return graphQLField.getReferenceType() == ReferenceType.REFERENCING
                  ? createPathInQueryToAlias(
                      newPathInQuery,
                      String.format("%s_%d", currentAlias, count.getAndIncrement()),
                      graphQLField.getForeignName().getTableName(),
                      subField,
                      executionConstants)
                  : new HashMap<String, String>();
            })
        .reduce(
            pathInQueryToAlias,
            (accumulated, current) -> {
              accumulated.putAll(current);
              return accumulated;
            });
  }

  private static Single<Map<String, Object>> executorListToSingleMap(
      List<Function<Row, Single<Map<String, Object>>>> executorList, Row row) {
    return Observable.fromIterable(executorList)
        .flatMapSingle(executor -> executor.apply(row))
        .collect(HashMap::new, Map::putAll);
  }

  public static Observable<Map<String, Object>> getRootResponse(
      String tableName, Field field, ExecutionConstants executionConstants, OpSpec opSpecExtra) {
    Map<String, String> pathInQueryToAlias =
        createPathInQueryToAlias(null, "t", tableName, field, executionConstants);

    Table table =
        new Table(
            tableName,
            pathInQueryToAlias.get(tableName),
            executionConstants.getRoleSpec(),
            OpSpecUtils.argumentsToOpSpec(field.getArguments(), tableName),
            opSpecExtra,
            executionConstants.getJwtParams(),
            pathInQueryToAlias);
    Query query = new Query(table);
    List<Function<Row, Single<Map<String, Object>>>> executorList =
        createExecutors(
            query.getTable(), field, query, executionConstants, pathInQueryToAlias, tableName);
    String queryString = query.createQuery();
    System.out.println(queryString);
    return executionConstants
        .getTransaction()
        .rxQuery(queryString)
        .flatMapObservable(Observable::fromIterable)
        .flatMapSingle(row -> executorListToSingleMap(executorList, row));
  }
}
