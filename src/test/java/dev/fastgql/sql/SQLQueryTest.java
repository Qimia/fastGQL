/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SQLQueryTest {

    SQLQuery sqlQuery;

    @BeforeEach
    public void setUp() {
        sqlQuery = new SQLQuery("mytable", "tablealias");
        sqlQuery.addKey("key1", "val1");
    }

    @Test
    public void testAddKey() {
        sqlQuery.addKey("key2", "val2");
        assertEquals("mytable", sqlQuery.build().substring(sqlQuery.build().indexOf("FROM") + 5).split(" ")[0]);
        assertEquals("tablealias", sqlQuery.build().substring(sqlQuery.build().indexOf("FROM") + 5).split(" ")[1]);
        assertTrue(sqlQuery.build().contains("key1.val1"));
        assertTrue(sqlQuery.build().contains("key2.val2"));
    }

    @Test
    public void testAddJoin() {
        sqlQuery.addJoin("mytable", "mykey", "foreignTable", "foreignAlias", "foreignKey");
        assertTrue(sqlQuery.build().contains("LEFT JOIN foreignTable foreignAlias ON mytable.mykey = foreignAlias.foreignKey"));
    }

    @Test void testAddSuffix() {
        sqlQuery.addSuffix("newSuffix");
        String[] res = sqlQuery.build().split(" ");
        assertTrue(res[res.length-1].contains("newSuffix"));
    }

    @Test
    public void testResetQuery() {
        sqlQuery.reset();
        assertFalse(sqlQuery.build().contains("key1.val1"));
    }
}
