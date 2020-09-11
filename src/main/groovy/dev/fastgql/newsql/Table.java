package dev.fastgql.newsql;

import dev.fastgql.dsl.OpSpec;
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
  private final List<String> allowedColumns;

  Table(
      String tableName,
      String tableAlias,
      RoleSpec roleSpec,
      OpSpec opSpecFromArguments,
      OpSpec opSpecExtra,
      Map<String, Object> jwtParams,
      Map<String, String> pathInQueryToAlias) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
    this.where =
        Stream.of(
                OpSpecUtils.conditionToSQL(
                    roleSpec.getTable(tableName).getOp(OpType.select).getCondition(),
                    tableAlias,
                    jwtParams),
                opSpecFromArguments == null
                    ? ""
                    : OpSpecUtils.conditionToSQL(
                        opSpecFromArguments.getCondition(), pathInQueryToAlias, jwtParams),
                opSpecExtra == null
                    ? ""
                    : OpSpecUtils.conditionToSQL(opSpecExtra.getCondition(), tableAlias, jwtParams))
            .filter(sqlString -> !sqlString.isEmpty())
            .collect(Collectors.joining(" AND "));
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

  public boolean isColumnAllowed(String column) {
    return allowedColumns.contains(column);
  }

  String sqlString() {
    return String.format("%s %s", tableName, tableAlias);
  }
}
