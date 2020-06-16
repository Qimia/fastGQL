/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentReferencedTest {

    @Test
    public void testUpdateQuery() {
        SQLQuery sqlQuery = new SQLQuery("mytable", "tablealias");
        sqlQuery.addKey("key1", "val1");
        sqlQuery.addKey("key2", "val2");

        ComponentReferenced componentReferenced = new ComponentReferenced("myFieldName", "myKeyName",
                "myForeignTable", "myForeignTableAlias", "myForeignKeyName");
        componentReferenced.setParentTableAlias("myPatentTable");
        componentReferenced.updateQuery(sqlQuery);

        assertTrue(sqlQuery.build().contains("myPatentTable.myKeyName AS myPatentTable_myKeyName"));
    }

    @Test
    public void testExtractValues() {
        // TODO
    }
}
