package dev.fastgql.sql;

public class LeftJoin {
  private final String foreignTableName;
  private final String foreignTableAlias;
  private final String tableAlias;
  private final String columnName;
  private final String foreignColumnName;

  LeftJoin(Table table, String columnName, Table foreignTable, String foreignColumnName) {
    this.foreignTableName = foreignTable.getTableName();
    this.foreignTableAlias = foreignTable.getTableAlias();
    this.tableAlias = table.getTableAlias();
    this.columnName = columnName;
    this.foreignColumnName = foreignColumnName;
  }

  LeftJoin(
      String foreignTableName,
      String foreignTableAlias,
      String tableAlias,
      String columnName,
      String foreignColumnName) {
    this.foreignTableName = foreignTableName;
    this.foreignTableAlias = foreignTableAlias;
    this.tableAlias = tableAlias;
    this.columnName = columnName;
    this.foreignColumnName = foreignColumnName;
  }

  public String getForeignTableName() {
    return foreignTableName;
  }

  public String getTableAlias() {
    return tableAlias;
  }

  public String getForeignTableAlias() {
    return foreignTableAlias;
  }

  public String getColumnName() {
    return columnName;
  }

  public String getForeignColumnName() {
    return foreignColumnName;
  }

  String sqlString() {
    return String.format(
        "LEFT JOIN %s %s ON %s.%s = %s.%s",
        foreignTableName,
        foreignTableAlias,
        tableAlias,
        columnName,
        foreignTableAlias,
        foreignColumnName);
  }

  @Override
  public String toString() {
    return "LeftJoin<"
        + "foreignTableName='"
        + foreignTableName
        + '\''
        + ", foreignTableAlias='"
        + foreignTableAlias
        + '\''
        + ", tableAlias='"
        + tableAlias
        + '\''
        + ", columnName='"
        + columnName
        + '\''
        + ", foreignColumnName='"
        + foreignColumnName
        + '\''
        + '>';
  }
}
