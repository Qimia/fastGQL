package dev.fastgql.dsl

import java.util.function.Function

class Condition {
    String column
    RelationalOperator operator
    Function<Map<String, Object>, Object> function
    LogicalConnective connective
    final String pathInQuery
    final List<Condition> next = new ArrayList<>()

    Condition(String pathInQuery) {
        this.pathInQuery = pathInQuery
    }

    Condition(String pathInQuery, String column, RelationalOperator operator, Function<Map<String, Object>, Object> function) {
        this.pathInQuery = pathInQuery
        this.column = column
        this.operator = operator
        this.function = function
    }

    @Override
    String toString() {
        "Condition<pathInQuery: ${pathInQuery}, column: ${column}, operator: ${operator}, function: ${function}, connective: ${connective}, next: ${next}>"
    }
}
