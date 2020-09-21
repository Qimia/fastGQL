package dev.fastgql.sql;

import dev.fastgql.common.RelationalOperator;
import dev.fastgql.dsl.LogicalConnective;
import dev.fastgql.graphql.GraphQLDatabaseSchema;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.Argument;
import graphql.language.ArrayValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.ObjectField;
import graphql.language.ObjectValue;
import graphql.language.StringValue;
import graphql.language.Value;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

class ConditionUtils {
  static Condition checkColumnIsEqValue(String columnName, Object value) {
    return new Condition(columnName, RelationalOperator._eq, params -> value);
  }

  public static Set<TableAlias> conditionToTableAliasSet(Condition condition, String tableAlias) {
    Set<TableAlias> ret = condition.getNext().stream()
      .map(it -> conditionToTableAliasSet(it, tableAlias))
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());

    Condition.Referencing referencing = condition.getReferencing();

    if (referencing != null) {
      String table = referencing.getForeignTable();
      String alias = String.format("%sr", tableAlias);
      ret.add(new TableAlias(table, alias));
      ret.addAll(conditionToTableAliasSet(referencing.getCondition(), alias));
    }
    return ret;
  }

  private static PreparedQuery conditionToSQLInternal(
      Condition condition, String tableAlias, Map<String, Object> jwtParams) {
    PreparedQuery nextPreparedQuery =
        condition.getNext().stream()
            .map(it -> conditionToSQLInternal(it, tableAlias, jwtParams))
            .collect(PreparedQuery.collector());

    PreparedQuery connective =
        condition.getConnective() != null
            ? PreparedQuery.create(condition.getConnective().toString().toUpperCase()).merge(" ")
            : PreparedQuery.create();

    PreparedQuery nextPreparedQueryWithSpace =
        !nextPreparedQuery.isEmpty()
            ? PreparedQuery.create(" ").merge(nextPreparedQuery)
            : nextPreparedQuery;

    Condition.Referencing referencing = condition.getReferencing();

    PreparedQuery not =
        condition.isNegated() ? PreparedQuery.create("NOT ") : PreparedQuery.create();

    PreparedQuery rootConditionPrepared = PreparedQuery.create();

    if (referencing != null) {
      String referencingAlias = String.format("%sr", tableAlias);
      rootConditionPrepared.merge(
          String.format(
              "EXISTS (SELECT 1 FROM %s %s WHERE %s.%s=%s.%s AND (",
              referencing.getForeignTable(),
              referencingAlias,
              referencingAlias,
              referencing.getForeignColumn(),
              tableAlias,
              referencing.getColumn()));
      rootConditionPrepared.merge(
          conditionToSQLInternal(referencing.getCondition(), referencingAlias, jwtParams));
      rootConditionPrepared.merge("))");
    } else if (condition.getColumn() != null
        && condition.getOperator() != null
        && condition.getFunction() != null) {
      Object value = condition.getFunction().apply(jwtParams);

      RelationalOperator operator = condition.getOperator();

      rootConditionPrepared
          .merge(tableAlias)
          .merge(".")
          .merge(condition.getColumn())
          .merge(condition.getOperator().getSql());

      switch (operator) {
        case _in:
        case _nin:
          rootConditionPrepared.merge("(");
          if (value instanceof List) {
            List<?> valueList = (List<?>) value;
            for (int i = 0; i < valueList.size(); i++) {
              rootConditionPrepared.addParam(valueList.get(i));
              if (i < valueList.size() - 1) {
                rootConditionPrepared.merge(", ");
              }
            }
          } else {
            rootConditionPrepared.addParam(value);
          }
          rootConditionPrepared.merge(")");
          break;
        default:
          rootConditionPrepared.addParam(value);
      }
    }

    if (!rootConditionPrepared.isEmpty()) {
      return not.isEmpty() && connective.isEmpty()
          ? rootConditionPrepared.merge(nextPreparedQueryWithSpace)
          : nextPreparedQueryWithSpace.isEmpty()
              ? not.merge(connective).merge(rootConditionPrepared)
              : not.merge(connective)
                  .merge(PreparedQuery.create("("))
                  .merge(rootConditionPrepared)
                  .merge(nextPreparedQueryWithSpace)
                  .merge(PreparedQuery.create(")"));
    }

    return connective.isEmpty() ? nextPreparedQuery : connective.merge(nextPreparedQuery);
  }

  static PreparedQuery conditionToSQL(
      Condition condition, String tableAlias, Map<String, Object> jwtParams) {
    return conditionToSQLInternal(condition, tableAlias, jwtParams);
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
      String name,
      ObjectValue objectValue,
      String pathInQuery,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    return objectValue.getObjectFields().stream()
        .map(
            objectField -> {
              if (List.of("_and", "_or", "_not").contains(objectField.getName())
                  || objectField.getValue() instanceof ObjectValue) {
                GraphQLField graphQLField = graphQLDatabaseSchema.fieldAt(pathInQuery, name);
                String foreignTable = graphQLField.getForeignName().getTableName();
                String foreignColumn = graphQLField.getForeignName().getKeyName();
                String column = graphQLField.getQualifiedName().getKeyName();
                Condition innerConditionReferencing =
                    createConditionFromObjectField(
                        objectField, foreignTable, graphQLDatabaseSchema);
                return Condition.createReferencing(
                    foreignTable, foreignColumn, column, innerConditionReferencing);
              } else {
                return new Condition(
                    name,
                    RelationalOperator.valueOf(objectField.getName()),
                    params -> valueToObject(objectField.getValue()));
              }
            })
        .reduce(conditionReducer(LogicalConnective.and))
        .orElseThrow();
  }

  private static Condition createArrayCondition(
      ArrayValue arrayValue,
      LogicalConnective logicalConnective,
      String pathInQuery,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    return arrayValue.getValues().stream()
        .map(
            node ->
                createConditionFromObjectValue(
                    (ObjectValue) node, pathInQuery, graphQLDatabaseSchema))
        .reduce(conditionReducer(logicalConnective))
        .orElseThrow();
  }

  private static Condition createConditionFromObjectField(
      ObjectField objectField, String pathInQuery, GraphQLDatabaseSchema graphQLDatabaseSchema) {
    switch (objectField.getName()) {
      case "_and":
        return objectField.getValue() instanceof ArrayValue
            ? createArrayCondition(
                (ArrayValue) objectField.getValue(),
                LogicalConnective.and,
                pathInQuery,
                graphQLDatabaseSchema)
            : createConditionFromObjectValue(
                (ObjectValue) objectField.getValue(), pathInQuery, graphQLDatabaseSchema);
      case "_or":
        return objectField.getValue() instanceof ArrayValue
            ? createArrayCondition(
                (ArrayValue) objectField.getValue(),
                LogicalConnective.or,
                pathInQuery,
                graphQLDatabaseSchema)
            : createConditionFromObjectValue(
                (ObjectValue) objectField.getValue(), pathInQuery, graphQLDatabaseSchema);
      case "_not":
        Condition notCondition = new Condition();
        Condition condition =
            createConditionFromObjectValue(
                (ObjectValue) objectField.getValue(), pathInQuery, graphQLDatabaseSchema);
        condition.setNegated(true);
        notCondition.getNext().add(condition);
        return notCondition;
      default:
        return createBasicCondition(
            objectField.getName(),
            (ObjectValue) objectField.getValue(),
            pathInQuery,
            graphQLDatabaseSchema);
    }
  }

  private static Condition createConditionFromObjectValue(
      ObjectValue objectValue, String pathInQuery, GraphQLDatabaseSchema graphQLDatabaseSchema) {
    return objectValue.getObjectFields().stream()
        .map(
            objectField ->
                createConditionFromObjectField(objectField, pathInQuery, graphQLDatabaseSchema))
        .reduce(conditionReducer(LogicalConnective.and))
        .orElseThrow();
  }

  static Condition createCondition(
      Argument argument, String tableName, GraphQLDatabaseSchema graphQLDatabaseSchema) {
    return createConditionFromObjectValue(
        (ObjectValue) argument.getValue(), tableName, graphQLDatabaseSchema);
  }
}
