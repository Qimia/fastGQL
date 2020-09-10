package dev.fastgql.dsl

class TableSpec {

    final Map<String, RoleSpec> roles = new HashMap<>()

    def role(String role, @DelegatesTo(strategy=Closure.DELEGATE_ONLY, value=RoleSpec) Closure cl) {
        def roleSpec = new RoleSpec()
        def code = cl.rehydrate(roleSpec, this, this)
        code.resolveStrategy = Closure.DELEGATE_ONLY
        code()
        roles.put(role, roleSpec)
    }

    RoleSpec getRole(String name) {
        return roles.get(name)
    }

    @Override
    String toString() {
        "TableSpec<roles: ${roles.toString()}>"
    }
}
