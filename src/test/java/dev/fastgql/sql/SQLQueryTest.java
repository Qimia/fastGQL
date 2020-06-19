/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonNull;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  public void testAddKey() throws NoSuchFieldException, IllegalAccessException {
    sqlQuery.addKey("table", "key");
    final Field field = getField("keys");
    assertTrue(((Set<?>) field.get(sqlQuery)).contains("table.key AS table_key"));
  }

  @Test
  public void testAddJoin() throws NoSuchFieldException, IllegalAccessException {
    sqlQuery.addJoin("thisTable", "thisKey", "foreignTable", "foreignTableAlias", "foreignKey");
    final Field field = getField("joins");
    assertTrue(
        ((List<?>) field.get(sqlQuery))
            .contains(
                "LEFT JOIN foreignTable foreignTableAlias ON thisTable.thisKey = foreignTableAlias.foreignKey"));
  }

  @Test
  public void testAddWhereConditions() throws NoSuchFieldException, IllegalAccessException {
    sqlQuery.addWhereConditions("whereConditional");
    final Field field = getField("whereConditions");
    assertTrue(((List<?>) field.get(sqlQuery)).contains("whereConditional"));
  }

  @Test
  public void testAddFieldToAlias() throws NoSuchFieldException, IllegalAccessException {
    sqlQuery.addFieldToAlias("field", "foreignTableAlias");
    final Field field = getField("fieldToAlias");
    assertEquals("foreignTableAlias", ((Map<?, ?>) field.get(sqlQuery)).get("field"));
  }

  @Test
  public void testBuild() {
    assertEquals(
        "SELECT table0.key0 AS table0_key0 FROM testTable testAlias LIMIT 1 OFFSET 2",
        sqlQuery.build());
  }

  @Test
  public void testReset() throws NoSuchFieldException, IllegalAccessException {
    sqlQuery.reset();
    final Field keysField = getField("keys");
    final Field joinsField = getField("joins");
    final Field whereConditionsField = getField("whereConditions");
    assertTrue(((Set<?>) keysField.get(sqlQuery)).isEmpty());
    assertTrue(((List<?>) joinsField.get(sqlQuery)).isEmpty());
    assertTrue(((List<?>) whereConditionsField.get(sqlQuery)).isEmpty());
  }

  private Field getField(String name) throws NoSuchFieldException {
    Field field = sqlQuery.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }
}
