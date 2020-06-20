/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.fastgql.TestUtils;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ComponentReferencedTest {

  static final String fieldName = "testFieldName";
  static final String keyName = "testKeyName";
  static final String foreignTable = "testForeignTable";
  static final String foreignTableAlias = "testForeignTableAlias";
  static final String foreignKeyName = "testForeignKeyName";
  static final String parentTableAlias = "testParentTableAlias";

  ComponentReferenced componentReferenced;

  @Mock SQLArguments sqlArguments;

  @BeforeEach
  public void setUp() {
    componentReferenced =
        new ComponentReferenced(
            fieldName, keyName, foreignTable, foreignTableAlias, foreignKeyName, sqlArguments);
  }

  @Test
  public void testConstructor() throws NoSuchFieldException, IllegalAccessException {
    assertEquals(keyName, TestUtils.getFieldByReflection(componentReferenced, "keyName"));
    assertEquals(fieldName, TestUtils.getFieldByReflection(componentReferenced, "fieldName"));
    assertEquals(
        foreignTableAlias,
        TestUtils.getFieldByReflection(componentReferenced, "foreignTableAlias"));
    assertEquals(
        foreignKeyName, TestUtils.getFieldByReflection(componentReferenced, "foreignKeyName"));
  }

  @Test
  public void testUpdateQueryWithNullParentTableAlias() {
    SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
    componentReferenced.updateQuery(sqlQuery);
    Mockito.verify(sqlQuery, Mockito.times(1))
        .addKey(
            ArgumentMatchers.argThat(Objects::isNull),
            ArgumentMatchers.argThat(argument -> argument.equals(keyName)));
  }

  @Test
  public void testUpdateQueryWithNotNullParentTableAlias()
      throws NoSuchFieldException, IllegalAccessException {
    // given
    SQLQuery sqlQuery = Mockito.mock(SQLQuery.class);
    Field parentTableAliasField = TestUtils.getField(componentReferenced, "parentTableAlias");
    parentTableAliasField.set(componentReferenced, parentTableAlias);
    // when
    componentReferenced.updateQuery(sqlQuery);
    // then
    Mockito.verify(sqlQuery, Mockito.times(1))
        .addKey(
            ArgumentMatchers.argThat(argument -> argument.equals(parentTableAlias)),
            ArgumentMatchers.argThat(argument -> argument.equals(keyName)));
  }

  @Test
  public void testSetParentTableAlias() throws NoSuchFieldException, IllegalAccessException {
    componentReferenced.setParentTableAlias(parentTableAlias);
    assertEquals(
        parentTableAlias, TestUtils.getFieldByReflection(componentReferenced, "parentTableAlias"));
  }

  @Test
  public void testExtractValuesShouldReturnEmptyMapResponse()
      throws NoSuchFieldException, IllegalAccessException {
    Field parentTableAliasField = TestUtils.getField(componentReferenced, "parentTableAlias");
    parentTableAliasField.set(componentReferenced, parentTableAlias);
    Map<String, Object> row = Map.of();
    componentReferenced.extractValues(row).test().assertNoErrors().assertValue(Map::isEmpty);
  }

  // TODO: test extractValues that return non trivial response
  @Test
  public void testExtractValuesShouldReturnNonTrivialResponse() {}

}
