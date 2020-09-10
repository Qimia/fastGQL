package dev.fastgql.newsql

import dev.fastgql.dsl.Condition
import dev.fastgql.dsl.LogicalConnective
import dev.fastgql.dsl.OpSpec
import dev.fastgql.dsl.RelationalOperator
import graphql.language.Argument
import graphql.language.ArrayValue
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.Node
import graphql.language.ObjectField
import graphql.language.ObjectValue
import graphql.language.StringValue
import graphql.language.Value

import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.stream.Collectors

class OpSpecUtils {
    static OpSpec checkColumnIsEqValue(String columnName, Object value) {
        Condition condition = new Condition()
        condition.setColumn(columnName)
        condition.setValue(value)
        condition.setOperator(RelationalOperator.eq)
        return new OpSpec(List.of(), List.of(), condition)
    }

    private static String relationalOperatorToString(RelationalOperator relationalOperator) {
        switch (relationalOperator) {
            case RelationalOperator.eq: return "="
            case RelationalOperator.neq: return "<>"
            case RelationalOperator.gt: return ">"
            case RelationalOperator.lt: return "<"
            case RelationalOperator.gte: return ">="
            case RelationalOperator.lte: return "<="
            default: ""
        }
    }

    private static RelationalOperator graphQLStringToRelationalOperator(String graphQLString) {
        switch (graphQLString) {
            case '_eq': return RelationalOperator.eq
            case '_neq': return RelationalOperator.neq
            case '_gt': return RelationalOperator.gt
            case '_lt': return RelationalOperator.lt
            case '_gte': return RelationalOperator.gte
            case '_lte': return RelationalOperator.lte
            default: throw new RuntimeException("not recognized operator: " + graphQLString)
        }
    }

    private static String conditionToSQLInternal(Condition condition, Function<Condition, String> conditionStringFunction, Map<String, Object> jwtParams) {
        String nextDescription = condition.getNext().stream()
                .map { conditionToSQLInternal(it, conditionStringFunction, jwtParams) }
                .collect(Collectors.joining(' '))
        String tableAlias = conditionStringFunction.apply(condition)
        String nextDescriptionWithSpace = nextDescription
                ? " ${nextDescription}"
                :""
        String connectiveDescription = condition.getConnective()
                ? "${condition.getConnective().toString().toUpperCase()} "
                :""
        Object value = condition.getValue() instanceof Function
                ? (condition.getValue() as Function<Map<String, Object>, Object>).apply(jwtParams)
                : condition.getValue()
        String valueDescription = value instanceof String
                ? "'${value}'"
                : "${value}"
        String relationalOperatorString = relationalOperatorToString(condition.getOperator())
        String rootConditionDescription = "${tableAlias}.${condition.getColumn()}${relationalOperatorString}${valueDescription}"
        String rootConditionDescriptionFormatted = condition.getNext().size() > 0
                ? "(${rootConditionDescription})"
                : rootConditionDescription
        return condition.getColumn() == null
                ? nextDescription
                : "${connectiveDescription}(${rootConditionDescriptionFormatted}${nextDescriptionWithSpace})"
    }

    static String conditionToSQL(Condition condition, Map<String, String> pathInQueryToAlias,
                                 Map<String, Object> jwtParams) {
        conditionToSQLInternal(condition, conditionArg -> pathInQueryToAlias.get(conditionArg.getPathInQuery()), jwtParams)
    }

    static String conditionToSQL(Condition condition, String alias, Map<String, Object> jwtParams) {
        conditionToSQLInternal(condition, conditionArg -> alias, jwtParams)
    }

    private static Object valueToObject(Value value) {
        if (value instanceof IntValue) {
            return value.getValue()
        } else if (value instanceof FloatValue) {
            return value.getValue()
        } else if (value instanceof StringValue) {
            return value.getValue()
        } else {
            throw new RuntimeException("not recognized object type: " + value.getClass())
        }
    }

    private static BinaryOperator<Condition> conditionReducer(LogicalConnective logicalConnective) {
        { Condition c1, Condition c2 ->
            c2.setConnective(logicalConnective)
            c1.getNext().add(c2)
            return c1
        }
    }

    private static Condition createBasicCondition(ObjectField objectField, String pathInQuery) {
        objectField.getValue().getChildren().stream()
            .map { it as ObjectField }
            .map {
                it.getValue() instanceof ObjectValue || it.getValue() instanceof ArrayValue
                    ? createConditionFromObjectField(it, String.format("%s/%s", pathInQuery, objectField.getName()))
                    : new Condition(pathInQuery, objectField.getName(),
                        graphQLStringToRelationalOperator(it.getName()), valueToObject(it.getValue()))
            }
            .reduce(conditionReducer(LogicalConnective.and))
            .get()
    }

    private static Condition createConditionFromObjectField(ObjectField objectField, String pathInQuery) {
        switch (objectField.getName()) {
            case "_and": return createArrayCondition(objectField.getValue().getChildren(), LogicalConnective.and, pathInQuery)
            case "_or": return createArrayCondition(objectField.getValue().getChildren(), LogicalConnective.or, pathInQuery)
            default: return createBasicCondition(objectField, pathInQuery)
        }
    }

    private static Condition createArrayCondition(List<Node> nodes, LogicalConnective logicalConnective, String pathInQuery) {
        nodes.stream()
            .map { it.getChildren() }
            .map { createConditionFromNodes(it, pathInQuery) }
            .reduce(conditionReducer(logicalConnective))
            .get()
    }

    private static Condition createConditionFromNodes(List<Node> nodes, String pathInQuery) {
        nodes.stream()
            .map { it as ObjectField }
            .map { createConditionFromObjectField(it, pathInQuery) }
            .reduce(conditionReducer(LogicalConnective.and))
            .get()
    }

    static OpSpec argumentsToOpSpec(List<Argument> arguments, String pathInQuery) {
        Optional<Map<String, Argument>> nameToArgumentOptional = arguments
            .stream()
            .map { Map.of(it.getName(), it) }
            .reduce { m1, m2 -> m1.putAll(m2)}

        Argument where = nameToArgumentOptional.isPresent()
                ? nameToArgumentOptional.get().where
                : null
        Condition condition = where
                ? createConditionFromNodes(where.getValue().getChildren(), pathInQuery)
                : new Condition(null)
        return new OpSpec(List.of(), List.of(), condition)
    }
}
