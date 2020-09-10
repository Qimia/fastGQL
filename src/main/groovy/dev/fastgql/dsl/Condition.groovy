package dev.fastgql.dsl

import java.util.function.Function
import java.util.stream.Collectors

class Condition {
    String column
    RelationalOperator operator
    Object value
    LogicalConnective connective
    final String pathInQuery
    final List<Condition> next = new ArrayList<>()

    Condition(String pathInQuery) {
        this.pathInQuery = pathInQuery
    }

    Condition(String pathInQuery, String column, RelationalOperator operator, Object value) {
        this.pathInQuery = pathInQuery
        this.column = column
        this.operator = operator
        this.value = value
    }

    @Override
    String toString() {
        "Condition<pathInQuery: ${pathInQuery}, column: ${column}, operator: ${operator}, value: ${value}, connective: ${connective}, next: ${next}>"
    }

    String describe() {
        String nextDescription = next.stream().map(condition -> condition.describe()).collect(Collectors.joining(" "))
        String nextDescriptionWithSpace = nextDescription? " ${nextDescription}":""
        String connectiveDescription = connective? "${connective} ":""
        String valueDescription = value instanceof Function ? "[function]" : "${value}"
        String rootConditionDescription = "${column} ${operator} ${valueDescription}"
        String rootConditionDescriptionFormatted = next.size() > 0 ? "(${rootConditionDescription})" : rootConditionDescription
        return column == null ? nextDescription : "${connectiveDescription}(${rootConditionDescriptionFormatted}${nextDescriptionWithSpace})"
    }
}
