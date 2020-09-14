package dev.fastgql.common;

public enum RelationalOperator {
  _eq("=", RelationalOperatorType.generic),
  _neq("<>", RelationalOperatorType.generic),
  _in(" IN ", RelationalOperatorType.generic),
  _nin(" NOT IN ", RelationalOperatorType.generic),
  _gt(">", RelationalOperatorType.generic),
  _lt("<", RelationalOperatorType.generic),
  _gte(">=", RelationalOperatorType.generic),
  _lte("<=", RelationalOperatorType.generic),
  _is_null(" IS NULL ", RelationalOperatorType.generic),
  _like(" LIKE ", RelationalOperatorType.text),
  _nlike(" NOT LIKE ", RelationalOperatorType.text),
  _ilike(" ILIKE ", RelationalOperatorType.text),
  _nilike(" NOT ILIKE ", RelationalOperatorType.text),
  _similar(" SIMILAR TO ", RelationalOperatorType.text),
  _nsimilar(" NOT SIMILAR TO ", RelationalOperatorType.text);

  private final String sql;
  private final RelationalOperatorType relationalOperatorType;

  RelationalOperator(String sql, RelationalOperatorType relationalOperatorType) {
    this.sql = sql;
    this.relationalOperatorType = relationalOperatorType;
  }

  public String getSql() {
    return sql;
  }

  public RelationalOperatorType getRelationalOperatorType() {
    return relationalOperatorType;
  }
}
