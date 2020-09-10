/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ComponentReferencedTest {

  private static final String tableName = "testTableName";
  private static final String tableAlias = "testTableAlias";
  private static final String fieldName = "testFieldName";
  private static final String keyName = "testKeyName";
  private static final String keyValue = "testKeyValue";
  private static final String foreignTable = "testForeignTable";
  private static final String foreignTableAlias = "testForeignTableAlias";
  private static final String foreignKeyName = "testForeignKeyName";
  private static final String parentTableAlias = "testParentTableAlias";

  @Test
  public void updateQuery() {
    ComponentReferenced componentReferenced =
        new ComponentReferenced(
            fieldName, keyName, foreignTable, foreignTableAlias, foreignKeyName, null, null, null);
    componentReferenced.setParentTableAlias(parentTableAlias);
    SQLQuery query = new SQLQuery(tableName, tableAlias, null);
    componentReferenced.updateQuery(query);
    assertEquals(
        String.format(
            "SELECT %s.%s AS %s_%s FROM %s %s ",
            parentTableAlias, keyName, parentTableAlias, keyName, tableName, tableAlias),
        query.build());
  }

  @Test
  public void extractValues_emptyRow() {
    ComponentReferenced componentReferenced =
        new ComponentReferenced(
            fieldName, keyName, foreignTable, foreignTableAlias, foreignKeyName, null, null, null);
    componentReferenced.setParentTableAlias(parentTableAlias);
    Map<String, Object> row = Map.of();
    componentReferenced.extractValues(null, row).test().assertNoErrors().assertValue(Map::isEmpty);
  }

  @Test
  public void extractValues_singleRow() {
    ComponentReferenced componentReferenced =
        new ComponentReferenced(
            fieldName, keyName, foreignTable, foreignTableAlias, foreignKeyName, null, null, null);
    componentReferenced.setParentTableAlias(parentTableAlias);
    SQLExecutor sqlExecutor =
        sqlQuery -> {
          assertEquals(
              String.format(
                  "SELECT FROM %s %s WHERE (%s.%s = %s) ",
                  foreignTable, foreignTableAlias, foreignTableAlias, foreignKeyName, keyValue),
              sqlQuery);
          return Single.just(List.of());
        };
    Map<String, Object> row = Map.of(String.format("%s_%s", parentTableAlias, keyName), keyValue);
    componentReferenced
        .extractValues(sqlExecutor, row)
        .test()
        .assertNoErrors()
        .assertValue(Map.of(fieldName, List.of()));
  }
}
