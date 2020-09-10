package dev.fastgql.dsl

class TableSpec {

    final OpType insert = OpType.insert
    final OpType select = OpType.select
    final OpType delete = OpType.delete
    final String name
    final Map<OpType, OpSpec> opSpecs = new HashMap<>()

    TableSpec(String name) {
        this.name = name
    }

    def ops(List<OpType> ops, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=OpSpec) Closure cl) {
        def opSpec = new OpSpec(name)
        def code = cl.rehydrate(opSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        ops.forEach { opSpecs.put(it, opSpec) }
    }

    OpSpec getOp(OpType opType) {
        return opSpecs.get(opType)
    }

    @Override
    String toString() {
        "TableSpec<opSpecs: ${opSpecs}>"
    }
}
