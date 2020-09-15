package dev.fastgql.security

import dev.fastgql.dsl.PermissionsConfig
import dev.fastgql.dsl.PermissionsSpec

class Permissions {
    static PermissionsSpec getPermissionsSpec() {
        PermissionsConfig.create().permissions {
            role ('default') {
                table ('customers') {
                    ops ([select, insert]) {
                        allow 'id', 'first_name'
                        check 'id' eq 1000 or 'id' eq 3003 or {
                            check 'first_name' eq 'bob' and 'id' eq 3333
                        }
                    }
                }
                table ('addresses') {
                    ops ([select]) {
                        allow 'id'
                        //check 'id' gt { it.id }
                    }
                }
                table ('phones') {
                    ops ([select]) {
                        allow 'id'
                        //check 'id' gt { it.id / 2 }
                    }
                }
            }
        }
    }
}
