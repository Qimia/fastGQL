/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SQLQueryTest {

  static SQLQuery sqlQuery;

  @BeforeAll
  static void setUp(@Mock SQLArguments sqlArguments) {
    sqlQuery = new SQLQuery("testTable", "testAlias", sqlArguments);
    Mockito.when(sqlArguments.getLimit()).thenReturn(1);
    Mockito.when(sqlArguments.getOffset()).thenReturn(2);
    Mockito.when(sqlArguments.getOrderBy()).thenReturn(JsonNull.INSTANCE);
    Mockito.when(sqlArguments.getWhere()).thenReturn(JsonNull.INSTANCE);
  }

  @BeforeEach
  void setUp() {
    sqlQuery.addKey("table0", "key0");
  }

  @AfterEach
  void tearDown() {
    sqlQuery.reset();
  }

  @Test
  public void testAddKey() {
    sqlQuery.addKey("table", "key");
    assertTrue(sqlQuery.build().contains("table.key AS table_key"));
  }

  @Test
  public void testAddJoin() {
    sqlQuery.addJoin("thisTable", "thisKey", "foreignTable", "foreignTableAlias", "foreignKey");
    assertTrue(
        sqlQuery
            .build()
            .contains(
                "LEFT JOIN foreignTable foreignTableAlias ON thisTable.thisKey = foreignTableAlias.foreignKey"));
  }

  @Test
  public void testAddWhereConditions() {
    sqlQuery.addWhereConditions("whereConditional");
    assertTrue(sqlQuery.build().contains("whereConditional"));
  }

  @Test
  public void testBuild() {
    assertEquals(
        "SELECT table0.key0 AS table0_key0 FROM testTable testAlias LIMIT 1 OFFSET 2",
        sqlQuery.build());
  }

  @Test
  public void testReset() {
    sqlQuery.reset();
    assertFalse(sqlQuery.build().contains("table0.key0 AS table0_key0"));
  }
}
