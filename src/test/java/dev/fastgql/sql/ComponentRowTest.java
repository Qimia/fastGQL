/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastgql.TestUtils;
import io.reactivex.Single;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ComponentRowTest {

  static final String keyName = "testKeyName";
  static final String parentTableAlias = "testParentTableAlias";
  ComponentRow componentRow;

  static Stream<Arguments> getTestExtractValues() {
    return Stream.of(
        Arguments.of(Map.of(), 0, null),
        Arguments.of(
            Map.of(String.format("%s_%s", parentTableAlias, keyName), "testValue"),
            1,
            "testValue"));
  }

  @BeforeEach
  public void setUp() {
    componentRow = new ComponentRow(keyName);
  }

  @Test
  public void testUpdateQuery() {
    SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
    componentRow.updateQuery(sqlQuery);
    ArgumentCaptor<String> argumentCaptor1 = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> argumentCaptor2 = ArgumentCaptor.forClass(String.class);
    Mockito.verify(sqlQuery, Mockito.times(1))
        .addKey(argumentCaptor1.capture(), argumentCaptor2.capture());
    assertNull(argumentCaptor1.getValue());
    assertEquals(keyName, argumentCaptor2.getValue());
  }

  @Test
  public void testSetParentTableAlias() throws NoSuchFieldException, IllegalAccessException {
    componentRow.setParentTableAlias(parentTableAlias);
    assertEquals(
        parentTableAlias, TestUtils.getFieldByReflection(componentRow, "parentTableAlias"));
  }

  @ParameterizedTest(name = "testExtractValues {index} => Test: [arguments]")
  @MethodSource("getTestExtractValues")
  public void testExtractValues(Map<String, Object> row, int size, Object testValue) {
    componentRow.setParentTableAlias(parentTableAlias);
    Single<Map<String, Object>> values = componentRow.extractValues(row);
    values
        .test()
        .assertNoErrors()
        .assertValue(v -> v.size() == size)
        .assertValue(v -> v.get(keyName) == testValue);
  }

  @Test
  public void testGetQueriedTables() {
    assertTrue(componentRow.getQueriedTables().isEmpty());
  }

  @Test
  public void testAddComponent() {
    Component component = Mockito.mock(Component.class);
    assertThrows(
        RuntimeException.class,
        () -> componentRow.addComponent(component),
        "ComponentRow cannot have any child components");
  }

  @Test
  public void testTableNameWhenParent() {
    assertThrows(
        RuntimeException.class,
        () -> componentRow.tableNameWhenParent(),
        "ComponentRow cannot have any child components");
  }
}
