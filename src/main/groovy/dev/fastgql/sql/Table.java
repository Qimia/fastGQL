package dev.fastgql.sql;

import dev.fastgql.dsl.OpType;
import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.dsl.TableSpec;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Table {
  private final String tableName;
  private final String tableAlias;
  private final String orderBy;
  private final String limit;
  private final String offset;
  private final List<String> allowedColumns;
  private final String conditionFromPermissions;
  private final String conditionFromArguments;
  private final Map<String, Object> jwtParams;
  private final String mockExtraCondition;
  private final String pathInQuery;
  private Condition extraCondition;

  Table(
      String tableName,
      String tableAlias,
      RoleSpec roleSpec,
      Arguments arguments,
      String mockExtraCondition,
      Map<String, Object> jwtParams,
      String pathInQuery,
      Map<String, String> pathInQueryToAlias) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
    if (roleSpec == null) {
      throw new RuntimeException(
          String.format("User role does not exist on table '%s'", tableName));
    }
    TableSpec tableSpec = roleSpec.getTable(tableName);
    if (tableSpec == null) {
      throw new RuntimeException(
          String.format("User does not have permissions defined for table '%s'", tableName));
    }
    this.allowedColumns = roleSpec.getTable(tableName).getOp(OpType.select).getAllowed();
    this.jwtParams = jwtParams;
    this.conditionFromPermissions =
        ConditionUtils.conditionToSQL(
            roleSpec.getTable(tableName).getOp(OpType.select).getCondition(),
            tableAlias,
            jwtParams);
    this.conditionFromArguments =
        arguments.getCondition() == null
            ? ""
            : ConditionUtils.conditionToSQL(
                arguments.getCondition(), pathInQueryToAlias, jwtParams);
    this.orderBy =
        arguments.getOrderByList() == null
            ? ""
            : OrderByUtils.orderByToSQL(arguments.getOrderByList(), pathInQueryToAlias);
    this.limit = arguments.getLimit() == null ? "" : arguments.getLimit().toString();
    this.offset = arguments.getOffset() == null ? "" : arguments.getOffset().toString();
    this.mockExtraCondition = mockExtraCondition;
    this.pathInQuery = pathInQuery;
  }

  public String getPathInQuery() {
    return pathInQuery;
  }

  public void setExtraCondition(Condition extraCondition) {
    this.extraCondition = extraCondition;
  }

  public String getTableName() {
    return tableName;
  }

  public String getTableAlias() {
    return tableAlias;
  }

  public String getWhere() {
    return Stream.of(
            conditionFromPermissions,
            conditionFromArguments,
            extraCondition == null
                ? ""
                : ConditionUtils.conditionToSQL(extraCondition, tableAlias, jwtParams))
        .filter(sqlString -> !sqlString.isEmpty())
        .map(sqlString -> String.format("(%s)", sqlString))
        .collect(Collectors.joining(" AND "));
  }

  public String getMockWhere() {
    return Stream.of(conditionFromPermissions, conditionFromArguments, mockExtraCondition)
        .filter(sqlString -> !sqlString.isEmpty())
        .map(sqlString -> String.format("(%s)", sqlString))
        .collect(Collectors.joining(" AND "));
  }

  public String getOrderBy() {
    return orderBy;
  }

  public String getLimit() {
    return limit;
  }

  public String getOffset() {
    return offset;
  }

  public boolean isColumnAllowed(String column) {
    return allowedColumns.contains(column);
  }

  String sqlString() {
    return String.format("%s %s", tableName, tableAlias);
  }
}
