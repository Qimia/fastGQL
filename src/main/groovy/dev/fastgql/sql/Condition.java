package dev.fastgql.sql;

import dev.fastgql.common.RelationalOperator;
import dev.fastgql.dsl.LogicalConnective;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Condition {

  static class Referencing {
    private final String foreignTable;
    private final String foreignColumn;
    private final String column;
    private final Condition condition;

    public Referencing(String foreignTable, String foreignColumn, String column, Condition condition) {
      this.foreignTable = foreignTable;
      this.foreignColumn = foreignColumn;
      this.column = column;
      this.condition = condition;
    }

    public String getForeignTable() {
      return foreignTable;
    }

    public String getColumn() {
      return column;
    }

    public String getForeignColumn() {
      return foreignColumn;
    }

    public Condition getCondition() {
      return condition;
    }

    @Override
    public String toString() {
      return "Referencing<" +
        "foreignTable: '" + foreignTable + '\'' +
        ", foreignColumn: '" + foreignColumn + '\'' +
        ", column: '" + column + '\'' +
        ", condition: " + condition +
        '>';
    }
  }

  private String column;
  private RelationalOperator operator;
  private Function<Map<String, Object>, Object> function;
  private LogicalConnective connective;
  private Referencing referencing;
  private boolean negated;
  private final List<Condition> next = new ArrayList<>();

  public Condition() {
    this.negated = false;
  }

  public Condition(
      String column,
      RelationalOperator operator,
      Function<Map<String, Object>, Object> function) {
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

  public void setReferencing(Referencing referencing) {
    this.referencing = referencing;
  }

  public Referencing getReferencing() {
    return referencing;
  }

  @Override
  public String toString() {
    return String.format(
        "Condition<referencing: %s, negated: %s, column: %s, operator: %s, function: %s, connective: %s, next: %s>",
        referencing, negated, column, operator, function, connective, next);
  }
}
