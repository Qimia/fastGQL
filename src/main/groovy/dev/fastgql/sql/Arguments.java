package dev.fastgql.sql;

import graphql.language.Argument;
import graphql.language.IntValue;
import java.math.BigInteger;
import java.util.List;

public class Arguments {

  private static final String WHERE = "where";
  private static final String ORDER_BY = "order_by";
  private static final String LIMIT = "limit";
  private static final String OFFSET = "offset";

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

  public Arguments(List<Argument> arguments, String pathInQuery) {
    Condition condition = null;
    List<OrderBy> orderByList = null;
    BigInteger limit = null;
    BigInteger offset = null;

    for (Argument argument : arguments) {
      switch (argument.getName()) {
        case WHERE:
          condition = ConditionUtils.createCondition(argument, pathInQuery);
          break;
        case ORDER_BY:
          orderByList = OrderByUtils.createOrderBy(argument, pathInQuery);
          break;
        case LIMIT:
          limit = ((IntValue) argument.getValue()).getValue();
          break;
        case OFFSET:
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
