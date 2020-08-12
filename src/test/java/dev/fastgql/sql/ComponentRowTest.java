/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.reactivex.Single;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ComponentRowTest {

  private static final String tableName = "testTableName";
  private static final String tableAlias = "testTableAlias";
  private static final String keyName = "testKeyName";
  private static final String keyValue = "testKeyValue";
  private static final String parentTableAlias = "testParentTableAlias";

  @Test
  public void updateQuery() {
    ComponentRow componentRow = new ComponentRow(keyName);
    componentRow.setParentTableAlias(parentTableAlias);
    SQLQuery sqlQuery = new SQLQuery(tableName, tableAlias, null);
    componentRow.updateQuery(sqlQuery);
    assertEquals(
        String.format(
            "SELECT %s.%s AS %s_%s FROM %s %s ",
            parentTableAlias, keyName, parentTableAlias, keyName, tableName, tableAlias),
        sqlQuery.build());
  }

  @Test
  public void extractValues_emptyRow() {
    ComponentRow componentRow = new ComponentRow(keyName);
    componentRow.setParentTableAlias(parentTableAlias);
    Map<String, Object> row = Map.of();
    Single<Map<String, Object>> values = componentRow.extractValues(null, row);
    values.test().assertNoErrors().assertValue(Map::isEmpty);
  }

  @Test
  public void extractValues_singleRow() {
    ComponentRow componentRow = new ComponentRow(keyName);
    componentRow.setParentTableAlias(parentTableAlias);
    Map<String, Object> row = Map.of(String.format("%s_%s", parentTableAlias, keyName), keyValue);
    Single<Map<String, Object>> values = componentRow.extractValues(null, row);
    values.test().assertNoErrors().assertValue(Map.of(keyName, keyValue));
  }

  @Test
  public void addComponent() {
    ComponentRow componentRow = new ComponentRow(keyName);
    Component component = Mockito.mock(Component.class);
    assertThrows(
        RuntimeException.class,
        () -> componentRow.addComponent(component),
        "ComponentRow cannot have any child components");
  }

  @Test
  public void tableNameWhenParent() {
    ComponentRow componentRow = new ComponentRow(keyName);
    assertThrows(
        RuntimeException.class,
        componentRow::tableNameWhenParent,
        "ComponentRow cannot have any child components");
  }
}
