package dev.fastgql.sql;

import dev.fastgql.common.RelationalOperator;
import dev.fastgql.dsl.LogicalConnective;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Condition {
  private String column;
  private RelationalOperator operator;
  private Function<Map<String, Object>, Object> function;
  private LogicalConnective connective;
  private boolean negated;
  private final String pathInQuery;
  private final List<Condition> next = new ArrayList<>();

  public Condition(String pathInQuery) {
    this.pathInQuery = pathInQuery;
    this.negated = false;
  }

  public Condition(
      String pathInQuery,
      String column,
      RelationalOperator operator,
      Function<Map<String, Object>, Object> function) {
    this.pathInQuery = pathInQuery;
    this.column = column;
    this.operator = operator;
    this.function = function;
    this.negated = false;
  }

  public String getColumn() {
    return column;
  }

  public RelationalOperator getOperator() {
    return operator;
  }

  public Function<Map<String, Object>, Object> getFunction() {
    return function;
  }

  public LogicalConnective getConnective() {
    return connective;
  }

  public String getPathInQuery() {
    return pathInQuery;
  }

  public boolean isNegated() {
    return negated;
  }

  public List<Condition> getNext() {
    return next;
  }

  public void setColumn(String column) {
    this.column = column;
  }

  public void setOperator(RelationalOperator operator) {
    this.operator = operator;
  }

  public void setFunction(Function<Map<String, Object>, Object> function) {
    this.function = function;
  }

  public void setConnective(LogicalConnective connective) {
    this.connective = connective;
  }

  public void setNegated(boolean negated) {
    this.negated = negated;
  }

  @Override
  public String toString() {
    return String.format(
        "Condition<pathInQuery: %s, negated: %s, column: %s, operator: %s, function: %s, connective: %s, next: %s>",
        pathInQuery, negated, column, operator, function, connective, next);
  }
}
