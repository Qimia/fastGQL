/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ComponentReferencedTest {

    @Test
    public void testUpdateQuery() {
        SQLQuery sqlQuery = new SQLQuery("mytable", "tablealias");
        sqlQuery.addKey("key1", "val1");
        sqlQuery.addKey("key2", "val2");
        System.out.println(sqlQuery.build());
        assertEquals("mytable", sqlQuery.build().substring(sqlQuery.build().indexOf("FROM") + 5).split(" ")[0]);
        assertEquals("tablealias", sqlQuery.build().substring(sqlQuery.build().indexOf("FROM") + 5).split(" ")[1]);
    }
}
