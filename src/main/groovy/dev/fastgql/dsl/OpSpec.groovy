package dev.fastgql.dsl

import java.util.function.Function

class OpSpec {

    static class Preset {
        final String column
        Object value

        Preset(String column) {
            this.column = column
        }

        def to(Object value) {
            this.value = value instanceof Closure
                ? value as Function<Map<String, Object>, Object>
                : value
        }

        @Override
        String toString() {
            "Preset<column: ${column}, value: ${value}>"
        }
    }

    static class ConditionBuilder {

        final Condition condition
        final Condition parent

        ConditionBuilder(Condition condition, Condition parent) {
            this.condition = condition
            this.parent = parent
        }

        private def handleOperator(RelationalOperator operator, Object value) {
            condition.setOperator(operator)
            value instanceof Closure
                    ? condition.setFunction(value as Function<Map<String, Object>, Object>)
                    : condition.setFunction { value }
            return new CheckChain(parent)
        }

        def eq(Object value) {
            return handleOperator(RelationalOperator.eq, value)
        }

        def neq(Object value) {
            return handleOperator(RelationalOperator.neq, value)
        }

        def lt(Object value) {
            return handleOperator(RelationalOperator.lt, value)
        }

        def gt(Object value) {
            return handleOperator(RelationalOperator.gt, value)
        }

        def lte(Object value) {
            return handleOperator(RelationalOperator.lte, value)
        }

        def gte(Object value) {
            return handleOperator(RelationalOperator.gte, value)
        }

    }

    static class CheckChain {
        final Condition condition

        CheckChain(Condition condition) {
            this.condition = condition
        }

        def handleLogicalConnective(LogicalConnective logicalConnective, String column) {
            Condition newCondition = new Condition(condition.getPathInQuery())
            newCondition.setColumn(column)
            newCondition.setConnective(logicalConnective)
            condition.getNext().add(newCondition)
            return new ConditionBuilder(newCondition, condition)
        }

        def handleLogicalConnective(LogicalConnective logicalConnective, Closure cl) {
            Condition newCondition = new Condition(condition.getPathInQuery())
            newCondition.setConnective(logicalConnective)
            condition.getNext().add(newCondition)
            def checkSpec = new CheckSpec(newCondition)
            cl.resolveStrategy = Closure.DELEGATE_ONLY
            def code = cl.rehydrate(checkSpec, this, this)
            code()
            return new CheckChain(condition)
        }

        def or(String column) {
            return handleLogicalConnective(LogicalConnective.or, column)
        }

        def or(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=CheckSpec) Closure cl) {
            return handleLogicalConnective(LogicalConnective.or, cl)
        }

        def and(String column) {
            return handleLogicalConnective(LogicalConnective.and, column)
        }

        def and(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=CheckSpec) Closure cl) {
            return handleLogicalConnective(LogicalConnective.and, cl)
        }
    }

    static class CheckSpec {

        final Condition condition

        CheckSpec(Condition condition) {
            this.condition = condition
        }

        def check(String column) {
            condition.setColumn(column)
            return new ConditionBuilder(condition, condition)
        }
    }

    final List<Preset> presets
    final List<String> allowed
    final Condition condition
    final String tableName

    OpSpec(String tableName) {
        this.presets = new ArrayList<>()
        this.allowed = new ArrayList<>()
        this.condition = new Condition(tableName)
        this.tableName = tableName
    }

    OpSpec(List<Preset> presets, List<String> allowed, Condition condition) {
        this.presets = presets
        this.allowed = allowed
        this.condition = condition
        this.tableName = null
    }

    def allow(String... columns) {
        this.allowed.addAll(columns)
    }

    def check(String column) {
        Condition newCondition = new Condition(tableName)
        if (condition.getNext().size() > 0) {
            newCondition.setConnective(LogicalConnective.and)
        }
        newCondition.setColumn(column)
        condition.getNext().add(newCondition)
        return new ConditionBuilder(newCondition, condition)
    }

    def check(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=CheckSpec) Closure cl) {
        Condition newCondition = new Condition(tableName)
        if (condition.getNext().size() > 0) {
            newCondition.setConnective(LogicalConnective.and)
        }
        condition.getNext().add(newCondition)
        def checkSpec = new CheckSpec(newCondition)
        cl.resolveStrategy = Closure.DELEGATE_ONLY
        def code = cl.rehydrate(checkSpec, this, this)
        code()
        return new CheckChain(condition)
    }

    def preset(String column) {
        def preset = new Preset(column)
        this.presets.add(preset)
        return preset
    }

    @Override
    String toString() {
        "OpSpec<allowed: ${allowed}, presets: ${presets}, conditions: ${condition.getNext()}>"
    }
}
