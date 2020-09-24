package dev.fastgql.sql;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderBy {
  private final List<LeftJoin> leftJoins;
  private final SelectColumn selectColumn;
  private final String order;

  OrderBy(List<LeftJoin> leftJoins, SelectColumn selectColumn, String order) {
    this.leftJoins = leftJoins;
    this.selectColumn = selectColumn;
    this.order = order.toUpperCase();
  }

  public String sqlString() {
    if (leftJoins.isEmpty()) {
      return String.format("%s %s", selectColumn.sqlString(), order);
    } else {
      LeftJoin firstLeftJoin = leftJoins.get(0);
      String nextLeftJoinsSql =
          leftJoins.stream().skip(1).map(LeftJoin::sqlString).collect(Collectors.joining(" "));
      return String.format(
          "(SELECT %s FROM %s %s %s WHERE %s.%s = %s.%s) %s",
          selectColumn.sqlString(),
          firstLeftJoin.getForeignTableName(),
          firstLeftJoin.getForeignTableAlias(),
          nextLeftJoinsSql,
          firstLeftJoin.getTableAlias(),
          firstLeftJoin.getColumnName(),
          firstLeftJoin.getForeignTableAlias(),
          firstLeftJoin.getForeignColumnName(),
          order);
    }
  }

  public Set<TableAlias> createTableAliasSet() {
    return leftJoins.stream()
        .map(
            leftJoin ->
                new TableAlias(leftJoin.getForeignTableName(), leftJoin.getForeignTableAlias()))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String toString() {
    return String.format(
        "OrderBy<leftJoins=%s, selectColumn=%s, order=%s>", leftJoins, selectColumn, order);
  }
}
