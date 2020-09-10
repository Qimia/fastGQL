package dev.fastgql

import dev.fastgql.dsl.PermissionsConfig
import dev.fastgql.dsl.PermissionsSpec

class Permissions {
    static PermissionsSpec permissions() {
        PermissionsConfig.create().permissions {
            table ('customers') {
                role ('default') {
                    ops ([select]) {
                        check 'id' gt 1
                    }
                }
            }
            table ('addresses') {
                role ('default') {
                    ops ([select]) {
                        check 'id' gt 90
                    }
                }
            }
        }
    }
}
