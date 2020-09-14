package dev.fastgql.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Query {
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

  private int resultAliasCount = 0;
  private final Table table;
  private final List<SelectColumn> selectColumns;
  private final List<LeftJoin> leftJoins;
  private final List<Table> queriedTables = new ArrayList<>();

  public Query(Table table) {
    this.table = table;
    this.selectColumns = new ArrayList<>();
    this.leftJoins = new ArrayList<>();
    this.queriedTables.add(this.table);
  }

  public Table getTable() {
    return table;
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

  private String createQueryInternal(Function<Table, String> tableStringFunction) {
    StringBuilder sqlStringBuilder = new StringBuilder();
    sqlStringBuilder.append(
        String.format(
            "SELECT %s FROM %s %s",
            selectColumns.stream().map(SelectColumn::sqlString).collect(Collectors.joining(", ")),
            table.sqlString(),
            leftJoins.stream().map(LeftJoin::sqlString).collect(Collectors.joining(" "))));

    String whereSqlString =
        queriedTables.stream()
            .map(tableStringFunction)
            .filter(where -> !where.isEmpty())
            .map(sqlString -> String.format("(%s)", sqlString))
            .collect(Collectors.joining(" AND "));
    String orderBySqlString = table.getOrderBy();
    String limitSqlString = table.getLimit();
    String offsetSqlString = table.getOffset();

    if (!whereSqlString.isEmpty()) {
      sqlStringBuilder.append(String.format(" WHERE %s", whereSqlString));
    }
    if (!orderBySqlString.isEmpty()) {
      sqlStringBuilder.append(String.format(" ORDER BY %s", orderBySqlString));
    }
    if (!limitSqlString.isEmpty()) {
      sqlStringBuilder.append(String.format(" LIMIT %s", limitSqlString));
    }
    if (!offsetSqlString.isEmpty()) {
      sqlStringBuilder.append(String.format(" OFFSET %s", offsetSqlString));
    }

    return sqlStringBuilder.toString();
  }

  public String createMockQuery() {
    return createQueryInternal(Table::getMockWhere);
  }

  public String createQuery() {
    return createQueryInternal(Table::getWhere);
  }
}
