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
                        //check 'id' eq { it.id }
                        //check 'address_ref/id' eq 5 and 'id' eq 10
                    }
                    ops ([insert]) {
                        allow 'name', 'address'
                        //preset 'id' to { it.id }
                        //check 'address' lt 100
                    }
                }
                table ('addresses') {
                    ops ([select]) {
                        allow 'id', 'street'
                        //check 'id' eq { it.id }
                    }
                    ops ([insert]) {
                        allow 'name', 'address'
                        //preset 'id' to { it.id }
                        //check 'address' lt 100
                    }
                }
            }
        }
    }
}
