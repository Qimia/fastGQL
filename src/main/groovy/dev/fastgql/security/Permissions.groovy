package dev.fastgql.security

import dev.fastgql.dsl.PermissionsConfig
import dev.fastgql.dsl.PermissionsSpec

class Permissions {
    static PermissionsSpec getPermissionsSpec() {
        PermissionsConfig.create().permissions {
            role ('default') {
                table ('customers') {
                    ops ([select]) {
                        allow 'id', 'first_name'
                        check 'id' _eq { it.id }
                        //check 'first_name' _similar 'John'
                        //check 'id' _in 101, 102
                        //check 'id' _eq 1 and {
                        //    check 'id' _lt 200 and {
                        //        check 'id' _gt 0 or 'id' _lt 200
                        //    }
                        //}
                    }
                }
                table ('addresses') {
                    ops ([select]) {
                        allow 'id'
                        //check 'id' _gt { it.id }
                    }
                }
                table ('phones') {
                    ops ([select]) {
                        allow 'id'
                        //check 'id' _gt { it.id / 2 }
                    }
                }
            }
        }
    }
}
