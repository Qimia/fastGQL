package dev.fastgql.common;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiFunction;

public enum RelationalOperator {
  _eq(
      "=",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && ((Comparable) left).compareTo(right) == 0),
  _neq(
      "<>",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && ((Comparable) left).compareTo(right) != 0),
  _in(
      " IN ",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && Arrays.stream((Object[]) right)
                  .anyMatch(element -> ((Comparable) left).compareTo(element) == 0)),
  _nin(
      " NOT IN ",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && Arrays.stream((Object[]) right)
                  .noneMatch(element -> ((Comparable) left).compareTo(element) == 0)),
  _gt(
      ">",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && ((Comparable) left).compareTo(right) > 0),
  _lt(
      "<",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && ((Comparable) left).compareTo(right) < 0),
  _gte(
      ">=",
      RelationalOperatorType.generic,
      ((left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && ((Comparable) left).compareTo(right) >= 0)),
  _lte(
      "<=",
      RelationalOperatorType.generic,
      (left, right) ->
          Objects.nonNull(left)
              && Objects.nonNull(right)
              && ((Comparable) left).compareTo(right) <= 0),
  _is_null(
      " IS NULL ",
      RelationalOperatorType.generic,
      (left, right) -> Objects.nonNull(right) && Objects.isNull(left) == (Boolean) right),
  _like(" LIKE ", RelationalOperatorType.text, (left, right) -> false),
  _nlike(" NOT LIKE ", RelationalOperatorType.text, (left, right) -> false),
  _ilike(" ILIKE ", RelationalOperatorType.text, (left, right) -> false),
  _nilike(" NOT ILIKE ", RelationalOperatorType.text, (left, right) -> false),
  _similar(" SIMILAR TO ", RelationalOperatorType.text, (left, right) -> false),
  _nsimilar(" NOT SIMILAR TO ", RelationalOperatorType.text, (left, right) -> false);

  private final String sql;
  private final RelationalOperatorType relationalOperatorType;
  private final BiFunction<Object, Object, Boolean> validator;

  RelationalOperator(
      String sql,
      RelationalOperatorType relationalOperatorType,
      BiFunction<Object, Object, Boolean> validator) {
    this.sql = sql;
    this.relationalOperatorType = relationalOperatorType;
    this.validator = validator;
  }

  public String getSql() {
    return sql;
  }

  public RelationalOperatorType getRelationalOperatorType() {
    return relationalOperatorType;
  }

  public BiFunction<Object, Object, Boolean> getValidator() {
    return validator;
  }
}
