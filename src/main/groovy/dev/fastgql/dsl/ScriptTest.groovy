package dev.fastgql.dsl

import dev.fastgql.newsql.OpSpecUtils

class ScriptTest {

    static void main(String[] args) {
        def result = PermissionsConfig.create().permissions {
            table('test') {
                role('default') {
                    ops([select]) {
                        //allow 'id', 'test'
                        //preset 'id' to { it.id }
                        //check { iss 'id' eq 8 or { iss 'id' eq 9 }} or { iss 'id' eq 10 }
                        //check { iss 'id' eq 8 } or { iss 'id' eq 9 } or { iss 'id' eq 10 }
                        //check { check 'id' eq { it.id } or 'id' eq 111 } or 'id' eq 11
                        //check 'id' eq {it.id} and 'id' gt 8 and { check 'id' eq 9 and 'id' eq 111 }
                        //check 'id' eq 4 and 'id3' eq 2
                        //check { check 'id' eq 5 or 'id' eq 6 }
                        //check 'id2' eq { it.id }
                        //check 'id' eq 5 or { check 'id' eq 6 or 'id' eq 7 }

                    }
                }
            }
        }
        Condition condition = result.getTable('test').getRole('default').getOp(RoleSpec.OpType.select).getCondition()
        println OpSpecUtils.conditionToSQL(condition, 't0', Map.of())
    }
}
