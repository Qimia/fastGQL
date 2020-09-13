package dev.fastgql.sql;

public class OrderBy {
  private final String pathInQuery;
  private final String column;
  private final String order;

  OrderBy(String pathInQuery, String column, String order) {
    this.pathInQuery = pathInQuery;
    this.column = column;
    this.order = order;
  }

  public String getPathInQuery() {
    return pathInQuery;
  }

  public String getColumn() {
    return column;
  }

  public String getOrder() {
    return order;
  }

  @Override
  public String toString() {
    return String.format(
        "OrderBy<pathInQuery=%s, column=%s, order=%s>", pathInQuery, column, order);
  }
}
