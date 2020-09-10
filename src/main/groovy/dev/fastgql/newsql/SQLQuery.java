package dev.fastgql.newsql;

import dev.fastgql.dsl.OpSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SQLQuery {
  static class Table {
    private final String tableName;
    private final String tableAlias;
    private final String where;

    Table(
        String tableName,
        String tableAlias,
        List<OpSpec> opSpecList,
        Map<String, Object> jwtParams) {
      this.tableName = tableName;
      this.tableAlias = tableAlias;
      this.where =
          opSpecList.stream()
              .map(
                  opSpec ->
                      OpSpecUtils.conditionToSQL(opSpec.getCondition(), tableAlias, jwtParams))
              .filter(sqlString -> !sqlString.isEmpty())
              .collect(Collectors.joining(" AND "));
    }

    public String getTableName() {
      return tableName;
    }

    private String getTableAlias() {
      return tableAlias;
    }

    private String getWhere() {
      return where;
    }

    String sqlString() {
      return String.format("%s %s", tableName, tableAlias);
    }
  }

  static class SelectColumn {

    private final String tableAlias;
    private final String columnName;
    private final String resultAlias;

    private SelectColumn(Table table, String columnName, String resultAlias) {
      this.tableAlias = table.getTableAlias();
      this.columnName = columnName;
      this.resultAlias = resultAlias;
    }

    private String sqlString() {
      return String.format("%s.%s AS %s", tableAlias, columnName, resultAlias);
    }

    public String getResultAlias() {
      return resultAlias;
    }
  }

  static class LeftJoin {
    private final Table table;
    private final String columnName;
    private final Table foreignTable;
    private final String foreignColumnName;

    LeftJoin(Table table, String columnName, Table foreignTable, String foreignColumnName) {
      this.table = table;
      this.columnName = columnName;
      this.foreignTable = foreignTable;
      this.foreignColumnName = foreignColumnName;
    }

    private String sqlString() {
      return String.format(
          "LEFT JOIN %s ON %s.%s = %s.%s",
          foreignTable.sqlString(),
          table.getTableAlias(),
          columnName,
          foreignTable.getTableAlias(),
          foreignColumnName);
    }
  }

  private int tableAliasCount = 0;
  private int resultAliasCount = 0;
  private final Table table;
  private final List<SelectColumn> selectColumns;
  private final List<LeftJoin> leftJoins;
  private final List<Table> queriedTables = new ArrayList<>();
  private final Map<String, Object> jwtParams;

  public SQLQuery(String tableName, List<OpSpec> opSpecList, Map<String, Object> jwtParams) {
    this.table = new Table(tableName, getNextTableAlias(), opSpecList, jwtParams);
    this.selectColumns = new ArrayList<>();
    this.leftJoins = new ArrayList<>();
    this.queriedTables.add(this.table);
    this.jwtParams = jwtParams;
  }

  public Table createNewTable(String tableName, List<OpSpec> opSpecList) {
    return new Table(tableName, getNextTableAlias(), opSpecList, jwtParams);
  }

  public Table getTable() {
    return table;
  }

  private String getNextTableAlias() {
    tableAliasCount++;
    return String.format("t%d", tableAliasCount);
  }

  private String getNextResultAlias() {
    resultAliasCount++;
    return String.format("v%d", resultAliasCount);
  }

  public SelectColumn addSelectColumn(Table table, String columnName) {
    SelectColumn selectColumn = new SelectColumn(table, columnName, getNextResultAlias());
    selectColumns.add(selectColumn);
    return selectColumn;
  }

  public SelectColumn addLeftJoin(
      Table table, String columnName, Table foreignTable, String foreignColumnName) {
    SelectColumn selectColumn = addSelectColumn(table, columnName);
    leftJoins.add(new LeftJoin(table, columnName, foreignTable, foreignColumnName));
    queriedTables.add(foreignTable);
    return selectColumn;
  }

  public String createQuery() {
    String baseString =
        String.format(
            "SELECT %s FROM %s %s",
            selectColumns.stream().map(SelectColumn::sqlString).collect(Collectors.joining(", ")),
            table.sqlString(),
            leftJoins.stream().map(LeftJoin::sqlString).collect(Collectors.joining(" ")));
    String whereSqlString =
        queriedTables.stream()
            .map(Table::getWhere)
            .filter(where -> !where.isEmpty())
            .collect(Collectors.joining(" AND "));
    if (!whereSqlString.isEmpty()) {
      return String.format("%s WHERE %s", baseString, whereSqlString);
    }
    return baseString;
  }
}
