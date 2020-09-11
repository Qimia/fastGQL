package dev.fastgql.newsql;

import dev.fastgql.dsl.LogicalConnective;
import dev.fastgql.dsl.RelationalOperator;
import graphql.language.*;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

class ConditionUtils {
  static Condition checkColumnIsEqValue(String columnName, Object value) {
    return new Condition(null, columnName, RelationalOperator.eq, params -> value);
  }

  private static String relationalOperatorToString(RelationalOperator relationalOperator) {
    switch (relationalOperator) {
      case eq:
        return "=";
      case neq:
        return "<>";
      case gt:
        return ">";
      case lt:
        return "<";
      case gte:
        return ">=";
      case lte:
        return "<=";
      default:
        return "";
    }
  }

  private static RelationalOperator graphQLStringToRelationalOperator(String graphQLString) {
    switch (graphQLString) {
      case "_eq":
        return RelationalOperator.eq;
      case "_neq":
        return RelationalOperator.neq;
      case "_gt":
        return RelationalOperator.gt;
      case "_lt":
        return RelationalOperator.lt;
      case "_gte":
        return RelationalOperator.gte;
      case "_lte":
        return RelationalOperator.lte;
      default:
        throw new RuntimeException("not recognized operator: " + graphQLString);
    }
  }

  private static String conditionToSQLInternal(
      Condition condition,
      Function<Condition, String> conditionStringFunction,
      Map<String, Object> jwtParams) {
    String nextDescription =
        condition.getNext().stream()
            .map(it -> conditionToSQLInternal(it, conditionStringFunction, jwtParams))
            .collect(Collectors.joining(" "));
    if (condition.getColumn() != null
        && condition.getOperator() != null
        && condition.getFunction() != null) {
      String tableAlias = conditionStringFunction.apply(condition);
      String nextDescriptionWithSpace =
          !nextDescription.isEmpty() ? String.format(" %s", nextDescription) : "";
      String connectiveDescription =
          condition.getConnective() != null
              ? String.format("%s ", condition.getConnective().toString().toUpperCase())
              : "";
      Object value = condition.getFunction().apply(jwtParams);
      String valueDescription =
          value instanceof String ? String.format("'%s'", value) : value.toString();
      String relationalOperatorString = relationalOperatorToString(condition.getOperator());
      String rootConditionDescription =
          String.format(
              "%s.%s%s%s",
              tableAlias, condition.getColumn(), relationalOperatorString, valueDescription);
      String rootConditionDescriptionFormatted =
          condition.getNext().size() > 0
              ? String.format("(%s)", rootConditionDescription)
              : rootConditionDescription;
      return String.format(
          "%s(%s%s)",
          connectiveDescription, rootConditionDescriptionFormatted, nextDescriptionWithSpace);
    } else {
      return nextDescription;
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

  private static Object objectFieldToObject(ObjectField objectField) {
    Value<?> value = objectField.getValue();
    if (value instanceof IntValue) {
      return ((IntValue) value).getValue();
    } else if (value instanceof FloatValue) {
      return ((FloatValue) value).getValue();
    } else if (value instanceof StringValue) {
      return ((StringValue) value).getValue();
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
    return objectValue.getChildren().stream()
        .map(node -> (ObjectField) node)
        .map(
            objectField ->
                objectField.getValue() instanceof ObjectValue
                        || objectField.getValue() instanceof ArrayValue
                    ? createConditionFromObjectField(
                        objectField, String.format("%s/%s", pathInQuery, name))
                    : new Condition(
                        pathInQuery,
                        name,
                        graphQLStringToRelationalOperator(objectField.getName()),
                        params -> objectFieldToObject(objectField)))
        .reduce(conditionReducer(LogicalConnective.and))
        .orElseThrow();
  }

  private static Condition createArrayCondition(
      ArrayValue arrayValue, LogicalConnective logicalConnective, String pathInQuery) {
    return arrayValue.getChildren().stream()
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
      default:
        return createBasicCondition(
            objectField.getName(), (ObjectValue) objectField.getValue(), pathInQuery);
    }
  }

  private static Condition createConditionFromObjectValue(
      ObjectValue objectValue, String pathInQuery) {
    return objectValue.getChildren().stream()
        .map(node -> createConditionFromObjectField((ObjectField) node, pathInQuery))
        .reduce(conditionReducer(LogicalConnective.and))
        .orElseThrow();
  }

  static Condition createCondition(Argument argument, String tableName) {
    return createConditionFromObjectValue((ObjectValue) argument.getValue(), tableName);
  }
}
