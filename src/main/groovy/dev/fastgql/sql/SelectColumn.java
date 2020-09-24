package dev.fastgql.sql;

public class SelectColumn {
  private final String tableAlias;
  private final String columnName;
  private final String resultAlias;

  SelectColumn(Table table, String columnName, String resultAlias) {
    this.tableAlias = table.getTableAlias();
    this.columnName = columnName;
    this.resultAlias = resultAlias;
  }

  SelectColumn(String tableAlias, String columnName) {
    this.tableAlias = tableAlias;
    this.columnName = columnName;
    this.resultAlias = null;
  }

  String sqlString() {
    if (resultAlias == null) {
      return String.format("%s.%s", tableAlias, columnName);
    } else {
      return String.format("%s.%s AS %s", tableAlias, columnName, resultAlias);
    }
  }

  public String getResultAlias() {
    return resultAlias;
  }

  @Override
  public String toString() {
    return "SelectColumn<"
        + "tableAlias='"
        + tableAlias
        + '\''
        + ", columnName='"
        + columnName
        + '\''
        + ", resultAlias='"
        + resultAlias
        + '\''
        + '>';
  }
}
