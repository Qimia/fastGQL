package dev.fastgql.dsl

class RoleSpec {

    final Map<String, TableSpec> tables = new HashMap<>()

    def table(String name, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=TableSpec) Closure cl) {
        def tableSpec = new TableSpec(name)
        def code = cl.rehydrate(tableSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        tables.put(name, tableSpec)
    }

    TableSpec getTable(String name) {
        return tables.get(name)
    }

    @Override
    String toString() {
        "RoleSpec<tables: ${tables}"
    }
}
