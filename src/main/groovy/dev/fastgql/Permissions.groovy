package dev.fastgql

import dev.fastgql.dsl.PermissionsConfig
import dev.fastgql.dsl.PermissionsSpec

class Permissions {
    static PermissionsSpec permissions() {
        PermissionsConfig.create().permissions {
            role ('default') {
                table ('customers') {
                    ops ([select]) {
                        allow 'id', 'first_name'
                        check 'id' gt 1 and {
                            check 'id' lt 200 and {
                                check 'id' gt 0 or 'id' lt 200
                            }
                        }
                    }
                }
                table ('addresses') {
                    ops ([select]) {
                        allow 'id'
                        check 'id' gt { it.id }
                    }
                }
                table ('phones') {
                    ops ([select]) {
                        allow 'id'
                        check 'id' gt { it.id / 2 }
                    }
                }
            }
        }
    }
}
