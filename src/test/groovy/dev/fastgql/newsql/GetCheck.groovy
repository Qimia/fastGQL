package dev.fastgql.newsql

import dev.fastgql.dsl.OpSpec
import dev.fastgql.dsl.OpSpec.Check

class GetCheck {
    static List<Check> getCheck() {
        OpSpec opSpec = new OpSpec()
        opSpec.check("id").is { eq 5 } and { eq 6 or { lt 7 } } or { eq 10 }
        return opSpec.getChecks()
    }
}
