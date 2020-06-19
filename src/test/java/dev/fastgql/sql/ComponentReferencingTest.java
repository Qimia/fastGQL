/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastgql.TestUtils;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ComponentReferencingTest {

  static final String fieldName = "testFieldName";
  static final String keyName = "testKeyName";
  static final String foreignTableName = "testForeignTableName";
  static final String foreignTableAlias = "testForeignTableAlias";
  static final String foreignKeyName = "testForeignKeyName";
  static final String parentTableAlias = "testParentTableAlias";
  ComponentReferencing componentReferencing;

  static Stream<Arguments> getTestExtractValues() {
    return Stream.of(Arguments.of(Map.of(), 0, null));
  }

  @BeforeEach
  public void setUp() {
    componentReferencing =
        new ComponentReferencing(
            fieldName, keyName, foreignTableName, foreignTableAlias, foreignKeyName);
  }

  @Test
  public void testConstructor() throws NoSuchFieldException, IllegalAccessException {
    assertEquals(fieldName, TestUtils.getFieldByReflection(componentReferencing, "fieldName"));
    assertEquals(keyName, TestUtils.getFieldByReflection(componentReferencing, "keyName"));
    assertEquals(
        foreignTableName, TestUtils.getFieldByReflection(componentReferencing, "foreignTableName"));
    assertEquals(
        foreignTableAlias,
        TestUtils.getFieldByReflection(componentReferencing, "foreignTableAlias"));
    assertEquals(
        foreignKeyName, TestUtils.getFieldByReflection(componentReferencing, "foreignKeyName"));
    assertTrue(
        ((List<?>) TestUtils.getFieldByReflection(componentReferencing, "components")).isEmpty());
    assertEquals(
        1, ((Set<?>) TestUtils.getFieldByReflection(componentReferencing, "queriedTables")).size());
    assertTrue(
        ((Set<?>) TestUtils.getFieldByReflection(componentReferencing, "queriedTables"))
            .contains(foreignTableName));
  }

  @Test
  public void testAddComponent() throws NoSuchFieldException, IllegalAccessException {
    // given
    Component component = Mockito.mock(Component.class);
    Set<String> expectedAddedQueriedTables = Set.of("testTable1", "testTable2");
    Mockito.when(component.getQueriedTables()).thenReturn(expectedAddedQueriedTables);

    // when
    componentReferencing.addComponent(component);

    // then
    Component mockComponentVerifierOneTime = Mockito.verify(component, Mockito.times(1));
    mockComponentVerifierOneTime.setParentTableAlias(
        ArgumentMatchers.argThat(argument -> argument.equals(foreignTableAlias)));
    mockComponentVerifierOneTime.setSqlExecutor(ArgumentMatchers.argThat(Objects::isNull));
    mockComponentVerifierOneTime.getQueriedTables();

    assertEquals(
        1, ((List<?>) TestUtils.getFieldByReflection(componentReferencing, "components")).size());
    assertEquals(
        component,
        ((List<?>) TestUtils.getFieldByReflection(componentReferencing, "components")).get(0));
    assertEquals(
        3, ((Set<?>) TestUtils.getFieldByReflection(componentReferencing, "queriedTables")).size());
    assertTrue(
        ((Set<?>) TestUtils.getFieldByReflection(componentReferencing, "queriedTables"))
            .containsAll(expectedAddedQueriedTables));
  }

  @Test
  public void testTableNameWhenParent() {
    assertEquals(foreignTableName, componentReferencing.tableNameWhenParent());
  }

  @Test
  public void testUpdateQuery() {
    // given
    SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
    Component component = Mockito.mock(Component.class);
    componentReferencing.addComponent(component);

    // when
    componentReferencing.updateQuery(sqlQuery);

    // then
    SQLQuery mockSqlQueryVerifierOneTime = Mockito.verify(sqlQuery, Mockito.times(1));
    mockSqlQueryVerifierOneTime.addKey(
        ArgumentMatchers.argThat(Objects::isNull),
        ArgumentMatchers.argThat(argument -> argument.equals(keyName)));
    mockSqlQueryVerifierOneTime.addJoin(
        ArgumentMatchers.argThat(Objects::isNull),
        ArgumentMatchers.argThat(argument -> argument.equals(keyName)),
        ArgumentMatchers.argThat(argument -> argument.equals(foreignTableName)),
        ArgumentMatchers.argThat(argument -> argument.equals(foreignTableAlias)),
        ArgumentMatchers.argThat(argument -> argument.equals(foreignKeyName)));
    mockSqlQueryVerifierOneTime.addFieldToAlias(
        ArgumentMatchers.argThat(argument -> argument.equals(fieldName)),
        ArgumentMatchers.argThat(argument -> argument.equals(foreignTableAlias)));
    Mockito.verify(component, Mockito.times(1))
        .updateQuery(ArgumentMatchers.argThat(argument -> argument.equals(sqlQuery)));
  }

  @Test
  public void testSetParentTableAlias() throws NoSuchFieldException, IllegalAccessException {
    componentReferencing.setParentTableAlias(parentTableAlias);
    assertEquals(
        parentTableAlias, TestUtils.getFieldByReflection(componentReferencing, "parentTableAlias"));
  }

  @Test
  public void testSetSqlExecutor() throws NoSuchFieldException, IllegalAccessException {
    // given
    SQLExecutor sqlExecutor = Mockito.mock(SQLExecutor.class);
    Component component = Mockito.mock(Component.class);
    componentReferencing.addComponent(component);

    // when
    componentReferencing.setSqlExecutor(sqlExecutor);

    // then
    assertEquals(sqlExecutor, TestUtils.getFieldByReflection(componentReferencing, "sqlExecutor"));
    Mockito.verify(component, Mockito.times(1))
        .setSqlExecutor(ArgumentMatchers.argThat(argument -> argument == sqlExecutor));
  }

  @ParameterizedTest(name = "testExtractValues {index} => Test: [arguments]")
  @MethodSource("getTestExtractValues")
  public void testExtractValues(Map<String, Object> row, int size, Object testValue) {
    // TODO: add parameters that give non-trivial return
    componentReferencing.setParentTableAlias(parentTableAlias);
    Single<Map<String, Object>> values = componentReferencing.extractValues(row);
    values
        .test()
        .assertNoErrors()
        .assertValue(v -> v.size() == size)
        .assertValue(v -> v.get(keyName) == testValue);
  }

  @Test
  public void testGetQueriedTables() {
    assertEquals(1, componentReferencing.getQueriedTables().size());
    assertTrue(componentReferencing.getQueriedTables().contains(foreignTableName));
  }
}
