package dev.fastgql.sql;

import dev.fastgql.common.RelationalOperator;
import dev.fastgql.dsl.LogicalConnective;
import graphql.language.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

class ConditionUtils {
  static Condition checkColumnIsEqValue(String columnName, Object value) {
    return new Condition(null, columnName, RelationalOperator._eq, params -> value);
  }

  private static String conditionToSQLInternal(
      Condition condition,
      Function<Condition, String> conditionStringFunction,
      Map<String, Object> jwtParams) {
    String nextDescription =
        condition.getNext().stream()
            .map(it -> conditionToSQLInternal(it, conditionStringFunction, jwtParams))
            .collect(Collectors.joining(" "));

    String connectiveDescription =
        condition.getConnective() != null
            ? String.format("%s ", condition.getConnective().toString().toUpperCase())
            : "";

    if (condition.getColumn() != null
        && condition.getOperator() != null
        && condition.getFunction() != null) {
      String tableAlias = conditionStringFunction.apply(condition);
      String nextDescriptionWithSpace =
          !nextDescription.isEmpty() ? String.format(" %s", nextDescription) : "";
      Object value = condition.getFunction().apply(jwtParams);
      String valueDescription = objectToSql(value);
      String relationalOperatorString = condition.getOperator().getSql();
      String rootConditionDescription =
          String.format(
              "%s.%s%s%s",
              tableAlias, condition.getColumn(), relationalOperatorString, valueDescription);
      String notDescription = condition.isNegated() ? "NOT " : "";
      return notDescription.isEmpty() && connectiveDescription.isEmpty()
          ? rootConditionDescription + nextDescriptionWithSpace
          : nextDescriptionWithSpace.isEmpty()
              ? String.format(
                  "%s%s%s", notDescription, connectiveDescription, rootConditionDescription)
              : String.format(
                  "%s%s(%s%s)",
                  notDescription,
                  connectiveDescription,
                  rootConditionDescription,
                  nextDescriptionWithSpace);
    } else {
      return connectiveDescription.isEmpty()
          ? nextDescription
          : String.format("%s%s", connectiveDescription, nextDescription);
    }
  }

  static String conditionToSQL(
      Condition condition, Map<String, String> pathInQueryToAlias, Map<String, Object> jwtParams) {
    return conditionToSQLInternal(
        condition,
        conditionArg -> pathInQueryToAlias.get(conditionArg.getPathInQuery()),
        jwtParams);
  }

  static String conditionToSQL(Condition condition, String alias, Map<String, Object> jwtParams) {
    return conditionToSQLInternal(condition, conditionArg -> alias, jwtParams);
  }

  private static String listToSql(List<?> list) {
    return String.format(
        "(%s)", list.stream().map(ConditionUtils::objectToSql).collect(Collectors.joining(", ")));
  }

  private static String objectToSql(Object object) {
    if (object instanceof String) {
      return String.format("'%s'", object);
    } else if (object instanceof List) {
      return listToSql((List<?>) object);
    } else if (object instanceof Object[]) {
      return listToSql(Arrays.asList((Object[]) object));
    } else {
      return String.valueOf(object);
    }
  }

  private static Object valueToObject(Value<?> value) {
    if (value instanceof IntValue) {
      return ((IntValue) value).getValue();
    } else if (value instanceof FloatValue) {
      return ((FloatValue) value).getValue();
    } else if (value instanceof StringValue) {
      return ((StringValue) value).getValue();
    } else if (value instanceof ArrayValue) {
      return ((ArrayValue) value)
          .getValues().stream().map(ConditionUtils::valueToObject).collect(Collectors.toList());
    } else {
      throw new RuntimeException("not recognized object type: " + value.getClass());
    }
  }

  private static BinaryOperator<Condition> conditionReducer(LogicalConnective logicalConnective) {
    return (Condition c1, Condition c2) -> {
      c2.setConnective(logicalConnective);
      c1.getNext().add(c2);
      return c1;
    };
  }

  private static Condition createBasicCondition(
      String name, ObjectValue objectValue, String pathInQuery) {
    return objectValue.getObjectFields().stream()
        .map(
            objectField ->
                objectField.getValue() instanceof ObjectValue
                        || List.of("_and", "_or", "_not").contains(objectField.getName())
                    ? createConditionFromObjectField(
                        objectField, String.format("%s/%s", pathInQuery, name))
                    : new Condition(
                        pathInQuery,
                        name,
                        RelationalOperator.valueOf(objectField.getName()),
                        params -> valueToObject(objectField.getValue())))
        .reduce(conditionReducer(LogicalConnective.and))
        .orElseThrow();
  }

  private static Condition createArrayCondition(
      ArrayValue arrayValue, LogicalConnective logicalConnective, String pathInQuery) {
    return arrayValue.getValues().stream()
        .map(node -> createConditionFromObjectValue((ObjectValue) node, pathInQuery))
        .reduce(conditionReducer(logicalConnective))
        .orElseThrow();
  }

  private static Condition createConditionFromObjectField(
      ObjectField objectField, String pathInQuery) {
    switch (objectField.getName()) {
      case "_and":
        return objectField.getValue() instanceof ArrayValue
            ? createArrayCondition(
                (ArrayValue) objectField.getValue(), LogicalConnective.and, pathInQuery)
            : createConditionFromObjectValue((ObjectValue) objectField.getValue(), pathInQuery);
      case "_or":
        return objectField.getValue() instanceof ArrayValue
            ? createArrayCondition(
                (ArrayValue) objectField.getValue(), LogicalConnective.or, pathInQuery)
            : createConditionFromObjectValue((ObjectValue) objectField.getValue(), pathInQuery);
      case "_not":
        Condition notCondition = new Condition(pathInQuery);
        Condition condition =
            createConditionFromObjectValue((ObjectValue) objectField.getValue(), pathInQuery);
        condition.setNegated(true);
        notCondition.getNext().add(condition);
        return notCondition;
      default:
        return createBasicCondition(
            objectField.getName(), (ObjectValue) objectField.getValue(), pathInQuery);
    }
  }

  private static Condition createConditionFromObjectValue(
      ObjectValue objectValue, String pathInQuery) {
    return objectValue.getObjectFields().stream()
        .map(objectField -> createConditionFromObjectField(objectField, pathInQuery))
        .reduce(conditionReducer(LogicalConnective.and))
        .orElseThrow();
  }

  static Condition createCondition(Argument argument, String tableName) {
    return createConditionFromObjectValue((ObjectValue) argument.getValue(), tableName);
  }
}
