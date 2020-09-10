package dev.fastgql.newsql;

import dev.fastgql.dsl.OpSpec;
import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Field;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLExecutionFunctions {

  private static Function<Row, Single<Map<String, Object>>> createExecutorForColumn(
      SQLQuery.Table table, GraphQLField graphQLField, SQLQuery sqlQuery) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    SQLQuery.SelectColumn selectColumn = sqlQuery.addSelectColumn(table, columnName);
    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null ? Single.just(Map.of()) : Single.just(Map.of(columnName, value));
    };
  }

  private static Function<Row, Single<Map<String, Object>>> createExecutorForReferencing(
      SQLQuery.Table table,
      Field field,
      GraphQLField graphQLField,
      SQLQuery sqlQuery,
      SQLExecutionConstants sqlExecutionConstants) {
    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();

    List<OpSpec> opSpecList =
        List.of(
            sqlExecutionConstants
                .getPermissionsSpec()
                .getTable(foreignTableName)
                .getRole(sqlExecutionConstants.getRole())
                .getOp(RoleSpec.OpType.select));

    SQLQuery.Table foreignTable = sqlQuery.createNewTable(foreignTableName, opSpecList);
    SQLQuery.SelectColumn selectColumnReferencing =
        sqlQuery.addLeftJoin(table, columnName, foreignTable, foreignColumnName);
    List<Function<Row, Single<Map<String, Object>>>> executors =
        createExecutors(foreignTable, field, sqlQuery, sqlExecutionConstants);
    return row -> {
      Object value = row.getValue(selectColumnReferencing.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : executorListToSingleMap(executors, row).map(result -> Map.of(field.getName(), result));
    };
  }

  private static Function<Row, Single<Map<String, Object>>> createExecutorForReferenced(
      SQLQuery.Table table,
      Field field,
      GraphQLField graphQLField,
      SQLQuery sqlQuery,
      SQLExecutionConstants sqlExecutionConstants) {

    String columnName = graphQLField.getQualifiedName().getKeyName();
    String foreignTableName = graphQLField.getForeignName().getTableName();
    String foreignColumnName = graphQLField.getForeignName().getKeyName();
    SQLQuery.SelectColumn selectColumn = sqlQuery.addSelectColumn(table, columnName);

    return row -> {
      Object value = row.getValue(selectColumn.getResultAlias());
      return value == null
          ? Single.just(Map.of())
          : getRootResponse(
                  foreignTableName,
                  field,
                  sqlExecutionConstants,
                  OpSpecUtils.checkColumnIsEqValue(foreignColumnName, value))
              .toList()
              .map(result -> Map.of(field.getName(), result));
    };
  }

  private static List<Function<Row, Single<Map<String, Object>>>> createExecutors(
      SQLQuery.Table table,
      Field field,
      SQLQuery sqlQuery,
      SQLExecutionConstants sqlExecutionConstants) {
    return field.getSelectionSet().getSelections().stream()
        .filter(selection -> selection instanceof Field)
        .map(selection -> (Field) selection)
        .map(
            subField -> {
              GraphQLField graphQLField =
                  sqlExecutionConstants
                      .getGraphQLDatabaseSchema()
                      .fieldAt(table.getTableName(), subField.getName());
              switch (graphQLField.getReferenceType()) {
                case NONE:
                  return createExecutorForColumn(table, graphQLField, sqlQuery);
                case REFERENCING:
                  return createExecutorForReferencing(
                      table, subField, graphQLField, sqlQuery, sqlExecutionConstants);
                case REFERENCED:
                  return createExecutorForReferenced(
                      table, subField, graphQLField, sqlQuery, sqlExecutionConstants);
                default:
                  throw new RuntimeException(
                      "Unknown reference type: " + graphQLField.getReferenceType());
              }
            })
        .collect(Collectors.toList());
  }

  private static Single<Map<String, Object>> executorListToSingleMap(
      List<Function<Row, Single<Map<String, Object>>>> executorList, Row row) {
    return Observable.fromIterable(executorList)
        .flatMapSingle(executor -> executor.apply(row))
        .collect(HashMap::new, Map::putAll);
  }

  public static Observable<Map<String, Object>> getRootResponse(
      String tableName,
      Field field,
      SQLExecutionConstants sqlExecutionConstants,
      OpSpec extraOpSpec) {
    OpSpec permissionsOpSpec =
        sqlExecutionConstants
            .getPermissionsSpec()
            .getTable(tableName)
            .getRole(sqlExecutionConstants.getRole())
            .getOp(RoleSpec.OpType.select);
    OpSpec argumentOpSpec = OpSpecUtils.argumentsToOpSpec(field.getArguments());
    List<OpSpec> opSpecList =
        extraOpSpec == null
            ? List.of(permissionsOpSpec, argumentOpSpec)
            : List.of(permissionsOpSpec, argumentOpSpec, extraOpSpec);

    SQLQuery sqlQuery = new SQLQuery(tableName, opSpecList, sqlExecutionConstants.getJwtParams());
    List<Function<Row, Single<Map<String, Object>>>> executorList =
        createExecutors(sqlQuery.getTable(), field, sqlQuery, sqlExecutionConstants);
    String query = sqlQuery.createQuery();
    System.out.println(query);
    return sqlExecutionConstants
        .getTransaction()
        .rxQuery(query)
        .flatMapObservable(Observable::fromIterable)
        .flatMapSingle(row -> executorListToSingleMap(executorList, row));
  }
}
