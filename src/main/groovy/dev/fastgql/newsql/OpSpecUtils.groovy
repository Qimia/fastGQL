package dev.fastgql.newsql

import dev.fastgql.dsl.Condition
import dev.fastgql.dsl.LogicalConnective
import dev.fastgql.dsl.OpSpec
import dev.fastgql.dsl.RelationalOperator
import graphql.language.Argument
import graphql.language.FloatValue
import graphql.language.IntValue
import graphql.language.Node
import graphql.language.ObjectField
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

    static String relationalOperatorToString(RelationalOperator relationalOperator) {
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

    static RelationalOperator graphQLStringToRelationalOperator(String graphQLString) {
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

    static String conditionToSQL(Condition condition, String tableAlias, Map<String, Object> jwtParams) {
        String nextDescription = condition.getNext().stream()
            .map { conditionToSQL(it, tableAlias, jwtParams) }
            .collect(Collectors.joining(' '))
        String nextDescriptionWithSpace = nextDescription? " ${nextDescription}":""
        String connectiveDescription = condition.getConnective()? "${condition.getConnective().toString().toUpperCase()} ":""
        Object value = condition.getValue() instanceof Function ? (condition.getValue() as Function<Map<String, Object>, Object>).apply(jwtParams) : condition.getValue()
        String valueDescription = value instanceof String ? "'${value}'" : "${value}"
        String rootConditionDescription = "${tableAlias}.${condition.getColumn()}${->relationalOperatorToString(condition.getOperator())}${valueDescription}"
        String rootConditionDescriptionFormatted = condition.getNext().size() > 0 ? "(${rootConditionDescription})" : rootConditionDescription
        return condition.getColumn() == null ? nextDescription : "${connectiveDescription}(${rootConditionDescriptionFormatted}${nextDescriptionWithSpace})"
    }

    static Object valueToObject(Value value) {
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

    static BinaryOperator<Condition> conditionReducer(LogicalConnective logicalConnective) {
        { Condition c1, Condition c2 ->
            c2.setConnective(logicalConnective)
            c1.getNext().add(c2)
            return c1
        }
    }

    static Condition createBasicCondition(ObjectField objectField) {
        objectField.getValue().getChildren().stream()
                .map { it as ObjectField }
                .map {
                    Condition condition = new Condition()
                    condition.setColumn(objectField.getName())
                    condition.setValue(valueToObject(it.getValue()))
                    condition.setOperator(graphQLStringToRelationalOperator(it.getName()))
                    return condition
                }
                .reduce(conditionReducer(LogicalConnective.and))
                .get()
    }

    static Condition createArrayCondition(List<Node> nodes, LogicalConnective logicalConnective) {
        nodes.stream()
                .map { it.getChildren() }
                .map { createCondition(it) }
                .reduce(conditionReducer(logicalConnective))
                .get()
    }

    static Condition createCondition(List<Node> nodes) {
        nodes.stream()
                .map { it as ObjectField }
                .map {
                    switch (it.getName()) {
                        case "_and": return createArrayCondition(it.getValue().getChildren(), LogicalConnective.and)
                        case "_or": return createArrayCondition(it.getValue().getChildren(), LogicalConnective.or)
                        default: return createBasicCondition(it)
                    }
                }
                .reduce(conditionReducer(LogicalConnective.and))
                .get()
    }

    static OpSpec argumentsToOpSpec(List<Argument> arguments) {
        Optional<Map<String, Argument>> nameToArgumentOptional = arguments
            .stream()
            .map { Map.of(it.getName(), it) }
            .reduce { m1, m2 -> m1.putAll(m2)}

        Argument where = nameToArgumentOptional.isPresent() ? nameToArgumentOptional.get().where : null
        Condition condition = where ? createCondition(where.getValue().getChildren()) : new Condition()
        return new OpSpec(List.of(), List.of(), condition)
    }
}
