package dev.fastgql.sql;

import java.util.Objects;

public class TableAlias {
  private final String tableName;
  private final String tableAlias;

  TableAlias(String tableName, String tableAlias) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
  }

  public String getTableName() {
    return tableName;
  }

  public String getTableAlias() {
    return tableAlias;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TableAlias that = (TableAlias) o;
    return Objects.equals(tableName, that.tableName) && Objects.equals(tableAlias, that.tableAlias);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableName, tableAlias);
  }
}
