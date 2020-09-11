package dev.fastgql.newsql;

import graphql.language.Argument;
import graphql.language.IntValue;

import java.math.BigInteger;
import java.util.List;

public class Arguments {

  private final Condition condition;
  private final List<OrderBy> orderByList;
  private final BigInteger limit;
  private final BigInteger offset;

  public Arguments() {
    this.condition = null;
    this.orderByList = null;
    this.limit = null;
    this.offset = null;
  }

  public Arguments(List<Argument> arguments, String tableName) {
    final String WHERE_NAME = "where";
    final String ORDER_BY_NAME = "order_by";
    final String LIMIT_NAME = "limit";
    final String OFFSET_NAME = "offset";
    Condition condition = null;
    List<OrderBy> orderByList = null;
    BigInteger limit = null;
    BigInteger offset = null;

    for (Argument argument : arguments) {
      switch (argument.getName()) {
        case WHERE_NAME:
          condition = ConditionUtils.createCondition(argument, tableName);
          break;
        case ORDER_BY_NAME:
          orderByList = OrderByUtils.createOrderBy(argument, tableName);
          break;
        case LIMIT_NAME:
          limit = ((IntValue) argument.getValue()).getValue();
          break;
        case OFFSET_NAME:
          offset = ((IntValue) argument.getValue()).getValue();
          break;
        default:
          break;
      }
    }
    this.condition = condition;
    this.orderByList = orderByList;
    this.limit = limit;
    this.offset = offset;
  }

  public Condition getCondition() {
    return condition;
  }

  public List<OrderBy> getOrderByList() {
    return orderByList;
  }

  public BigInteger getLimit() {
    return limit;
  }

  public BigInteger getOffset() {
    return offset;
  }
}
