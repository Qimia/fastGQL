/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AliasGeneratorTest {

    @Test
    public void testGetAlias() {
        AliasGenerator aliasGenerator = new AliasGenerator();
        assertEquals("a0", aliasGenerator.getAlias());
        assertEquals("a1", aliasGenerator.getAlias());
        assertEquals("a2", aliasGenerator.getAlias());
    }
}
