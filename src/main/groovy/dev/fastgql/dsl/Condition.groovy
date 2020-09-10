package dev.fastgql.dsl

import java.util.function.Function
import java.util.stream.Collectors

class Condition {
    String column
    RelationalOperator operator
    Object value
    LogicalConnective connective
    final List<Condition> next = new ArrayList<>()

    @Override
    String toString() {
        "Condition<column: ${column}, operator: ${operator}, value: ${value}, connective: ${connective}, next: ${next}>"
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
