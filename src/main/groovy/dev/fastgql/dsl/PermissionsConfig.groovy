package dev.fastgql.dsl

abstract class PermissionsConfig extends Script {

    def permissions(@DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=PermissionsSpec) Closure cl) {
        def permissionsSpec = new PermissionsSpec()
        def code = cl.rehydrate(permissionsSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        permissionsSpec
    }

    static PermissionsConfig create() {
        new PermissionsConfig() {
            @Override
            Object run() {
                return null
            }
        }
    }
}
