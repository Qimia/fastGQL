package dev.fastgql.security

import dev.fastgql.dsl.OpType
import dev.fastgql.dsl.PermissionsConfig
import dev.fastgql.dsl.PermissionsSpec

class Permissions {
    static PermissionsSpec getPermissionsSpec() {
        PermissionsConfig.create().permissions {
            role ('default') {
                table ('customers') {
                    ops ([select]) {
                        allow 'id', 'name', 'address'
                        check 'id' eq { it.id }
                    }
                    ops ([insert]) {
                        allow 'name', 'address'
                        preset 'id' to { it.id }
                        check 'address' lt 100
                    }
                }
            }
        }
    }
}
