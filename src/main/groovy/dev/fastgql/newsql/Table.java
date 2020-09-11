package dev.fastgql.newsql;

import dev.fastgql.dsl.OpType;
import dev.fastgql.dsl.RoleSpec;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Table {
  private final String tableName;
  private final String tableAlias;
  private final String where;
  private final String orderBy;
  private final String limit;
  private final String offset;
  private final List<String> allowedColumns;

  Table(
      String tableName,
      String tableAlias,
      RoleSpec roleSpec,
      Arguments arguments,
      Condition extraCondition,
      Map<String, Object> jwtParams,
      Map<String, String> pathInQueryToAlias) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
    this.where =
      Stream.of(
        ConditionUtils.conditionToSQL(
          roleSpec.getTable(tableName).getOp(OpType.select).getCondition(),
          tableAlias,
          jwtParams),
        arguments.getCondition() == null
          ? ""
          : ConditionUtils.conditionToSQL(
          arguments.getCondition(), pathInQueryToAlias, jwtParams),
        extraCondition == null
          ? ""
          : ConditionUtils.conditionToSQL(extraCondition, tableAlias, jwtParams))
        .filter(sqlString -> !sqlString.isEmpty())
        .collect(Collectors.joining(" AND "));
    this.orderBy =
      arguments.getOrderByList() == null
        ? ""
        : OrderByUtils.orderByToSQL(arguments.getOrderByList(), pathInQueryToAlias);
    this.limit = arguments.getLimit() == null ? "" : arguments.getLimit().toString();
    this.offset = arguments.getOffset() == null ? "" : arguments.getOffset().toString();
    this.allowedColumns = roleSpec.getTable(tableName).getOp(OpType.select).getAllowed();
  }

  public String getTableName() {
    return tableName;
  }

  public String getTableAlias() {
    return tableAlias;
  }

  public String getWhere() {
    return where;
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
