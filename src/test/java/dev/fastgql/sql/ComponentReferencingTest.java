/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.reactivex.Single;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ComponentReferencingTest {

  private static final String fieldName = "testFieldName";
  private static final String keyName = "testKeyName";
  private static final String keyValue = "testKeyValue";
  private static final String foreignTableName = "testForeignTableName";
  private static final String foreignTableAlias = "testForeignTableAlias";
  private static final String tableName = "testTableName";
  private static final String tableAlias = "testTableAlias";
  private static final String foreignKeyName = "testForeignKeyName";
  private static final String foreignKeyValue = "testForeignKeyValue";
  private static final String parentTableAlias = "testParentTableAlias";

  @Test
  public void updateQuery() {
    ComponentReferencing componentReferencing =
        new ComponentReferencing(
            fieldName, keyName, foreignTableName, foreignTableAlias, foreignKeyName);
    componentReferencing.setParentTableAlias(parentTableAlias);
    SQLQuery query = new SQLQuery(tableName, tableAlias, null);
    componentReferencing.updateQuery(query);
    assertEquals(
        String.format(
            "SELECT %s.%s AS %s_%s FROM %s %s LEFT JOIN %s %s ON %s.%s = %s.%s ",
            parentTableAlias,
            keyName,
            parentTableAlias,
            keyName,
            tableName,
            tableAlias,
            foreignTableName,
            foreignTableAlias,
            parentTableAlias,
            keyName,
            foreignTableAlias,
            foreignKeyName),
        query.build());
  }

  @Test
  public void extractValues_emptyRow() {
    ComponentReferencing componentReferencing =
        new ComponentReferencing(
            fieldName, keyName, foreignTableName, foreignTableAlias, foreignKeyName);
    componentReferencing.setParentTableAlias(parentTableAlias);
    Map<String, Object> row = Map.of();
    Single<Map<String, Object>> values = componentReferencing.extractValues(null, row);
    values.test().assertNoErrors().assertValue(Map::isEmpty);
  }

  @Test
  public void extractValues_singeRow() {
    ComponentReferencing componentReferencing =
        new ComponentReferencing(
            fieldName, keyName, foreignTableName, foreignTableAlias, foreignKeyName);
    ComponentRow componentRow = new ComponentRow(foreignKeyName);
    componentReferencing.addComponent(componentRow);
    componentReferencing.setParentTableAlias(parentTableAlias);
    Map<String, Object> row =
        Map.of(
            String.format("%s_%s", parentTableAlias, keyName),
            keyValue,
            String.format("%s_%s", foreignTableAlias, foreignKeyName),
            foreignKeyValue);
    Single<Map<String, Object>> values = componentReferencing.extractValues(null, row);
    values
        .test()
        .assertNoErrors()
        .assertValue(Map.of(fieldName, Map.of(foreignKeyName, foreignKeyValue)));
  }
}
