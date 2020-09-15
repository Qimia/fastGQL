package dev.fastgql.dsl

import dev.fastgql.common.RelationalOperator
import dev.fastgql.sql.Condition
import dev.fastgql.sql.Preset

import java.util.function.Function
import java.util.stream.Collectors

class OpSpec {

    static class PresetSpec {

        final String column;
        Function<Map<String, Object>, Object> value;

        PresetSpec(String column) {
            this.column = column
        }

        def to(Object value) {
            this.value = value instanceof Closure
                    ? value as Function<Map<String, Object>, Object>
                    : { value } as Function<Map<String, Object>, Object>
        }

        @Override
        String toString() {
            "PresetSpec<column: ${column}, value: ${value}>"
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
            return handleOperator(RelationalOperator._eq, value)
        }

        def neq(Object value) {
            return handleOperator(RelationalOperator._neq, value)
        }

        def within(Object... value) {
            return handleOperator(RelationalOperator._in, value)
        }

        def nin(Object... value) {
            return handleOperator(RelationalOperator._nin, value)
        }

        def gt(Object value) {
            return handleOperator(RelationalOperator._gt, value)
        }

        def lt(Object value) {
            return handleOperator(RelationalOperator._lt, value)
        }

        def gte(Object value) {
            return handleOperator(RelationalOperator._gte, value)
        }

        def lte(Object value) {
            return handleOperator(RelationalOperator._lte, value)
        }

        def is_null(boolean value) {
            return handleOperator(RelationalOperator._is_null, value)
        }

        def like(String value) {
            return handleOperator(RelationalOperator._like, value)
        }

        def nlike(String value) {
            return handleOperator(RelationalOperator._nlike, value)
        }

        def ilike(String value) {
            return handleOperator(RelationalOperator._ilike, value)
        }

        def nilike(String value) {
            return handleOperator(RelationalOperator._nilike, value)
        }

        def similar(String value) {
            return handleOperator(RelationalOperator._similar, value)
        }

        def nsimilar(String value) {
            return handleOperator(RelationalOperator._nsimilar, value)
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

    final List<PresetSpec> presets
    final List<String> allowed
    final Condition condition
    final String tableName

    OpSpec(String tableName) {
        this.presets = new ArrayList<>()
        this.allowed = new ArrayList<>()
        this.condition = new Condition(tableName)
        this.tableName = tableName
    }

    OpSpec(List<PresetSpec> presets, List<String> allowed, Condition condition) {
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
        def preset = new PresetSpec(column)
        this.presets.add(preset)
        return preset
    }

    List<Preset> createPresets() {
        return presets.stream().map { it -> new Preset(it.column, it.value) }
            .collect(Collectors.toList())
    }

    @Override
    String toString() {
        "OpSpec<allowed: ${allowed}, presets: ${presets}, conditions: ${condition.getNext()}>"
    }
}
