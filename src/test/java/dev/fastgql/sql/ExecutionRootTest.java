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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ExecutionRootTest {

  static final String tableName = "testTableName";
  static final String tableAlias = "testTableAlias";

  ExecutionRoot executionRoot;

  @Mock SQLArguments sqlArguments = new SQLArguments(Map.of());

  @BeforeEach
  public void setUp() {
    executionRoot = new ExecutionRoot(tableName, tableAlias, sqlArguments);
  }

  @Test
  public void testConstructor() throws NoSuchFieldException, IllegalAccessException {
    assertEquals(tableName, TestUtils.getFieldByReflection(executionRoot, "tableName"));
    assertEquals(tableAlias, TestUtils.getFieldByReflection(executionRoot, "tableAlias"));
    assertTrue(((List<?>) TestUtils.getFieldByReflection(executionRoot, "components")).isEmpty());
    Set<?> queriedTables = (Set<?>) TestUtils.getFieldByReflection(executionRoot, "queriedTables");
    assertEquals(1, queriedTables.size());
    assertTrue(queriedTables.contains(tableName));
    SQLQuery query = (SQLQuery) TestUtils.getFieldByReflection(executionRoot, "query");
    assertEquals(tableName, TestUtils.getFieldByReflection(query, "table"));
    assertEquals(tableAlias, TestUtils.getFieldByReflection(query, "alias"));
    assertEquals(sqlArguments, TestUtils.getFieldByReflection(query, "args"));
    assertEquals(
        "dev.fastgql.sql.SQLExecutor",
        TestUtils.getFieldByReflection(executionRoot, "sqlExecutor").getClass().getName());
  }

  @Test
  public void testExecute() {
    List<Map<String, Object>> forged =
        List.of(
            Map.of("testTableAlias_id", 101, "testTableAlias_first_name", "John"),
            Map.of("testTableAlias_id", 102, "testTableAlias_first_name", "Mike"));
    List<Map<String, Object>> expected =
        List.of(Map.of("id", 101, "first_name", "John"), Map.of("id", 102, "first_name", "Mike"));

    executionRoot.setSqlExecutor(new SQLExecutor(query -> Single.just(forged)));
    executionRoot.addComponent(new ComponentRow("id"));
    executionRoot.addComponent(new ComponentRow("first_name"));

    executionRoot.execute().test().assertNoErrors().assertValue(v -> v.equals(expected));
  }

  @Test
  public void testAddComponent() throws NoSuchFieldException, IllegalAccessException {
    // given
    Component component = Mockito.mock(Component.class);
    SQLExecutor sqlExecutor = Mockito.mock(SQLExecutor.class);
    List<Component> spyComponents = Mockito.spy(new ArrayList<>());
    TestUtils.setFieldByReflection(executionRoot, "components", spyComponents);
    TestUtils.setFieldByReflection(executionRoot, "sqlExecutor", sqlExecutor);
    // when
    executionRoot.addComponent(component);
    // then
    InOrder componentInOrder = Mockito.inOrder(component);
    componentInOrder.verify(component, Mockito.times(1)).setParentTableAlias(tableAlias);
    componentInOrder.verify(component, Mockito.times(1)).setSqlExecutor(sqlExecutor);
    Mockito.verify(spyComponents, Mockito.times(1)).add(component);
    assertEquals(1, spyComponents.size());
    Mockito.verify(component, Mockito.times(1)).getQueriedTables();
  }

  @Test
  public void testTableNameWhenParent() {
    assertEquals(tableName, executionRoot.tableNameWhenParent());
  }

  @Test
  public void testSetSqlExecutor() throws NoSuchFieldException, IllegalAccessException {
    // given
    SQLExecutor sqlExecutor = Mockito.mock(SQLExecutor.class);
    Component component = Mockito.mock(Component.class);
    List<Component> mockComponents = List.of(component);
    TestUtils.setFieldByReflection(executionRoot, "components", mockComponents);
    // when
    executionRoot.setSqlExecutor(sqlExecutor);
    // then
    assertEquals(sqlExecutor, TestUtils.getFieldByReflection(executionRoot, "sqlExecutor"));
    Mockito.verify(component, Mockito.times(1)).setSqlExecutor(sqlExecutor);
  }

  @Test
  public void testGetQueriedTables() {
    Set<String> queriedTables = executionRoot.getQueriedTables();
    assertTrue(queriedTables.contains(tableName));
    assertEquals(1, queriedTables.size());
  }
}
