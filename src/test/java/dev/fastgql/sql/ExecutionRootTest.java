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
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ExecutionRootTest {

  static final String tableName = "testTableName";
  static final String tableAlias = "testTableAlias";

  @Test
  public void execute() {
    ExecutionRoot executionRoot = new ExecutionRoot(tableName, tableAlias, null);
    List<Map<String, Object>> forged =
        List.of(
            Map.of("testTableAlias_id", 101, "testTableAlias_first_name", "John"),
            Map.of("testTableAlias_id", 102, "testTableAlias_first_name", "Mike"));
    List<Map<String, Object>> expected =
        List.of(Map.of("id", 101, "first_name", "John"), Map.of("id", 102, "first_name", "Mike"));

    executionRoot.setSqlExecutor(query -> Single.just(forged));
    executionRoot.addComponent(new ComponentRow("id"));
    executionRoot.addComponent(new ComponentRow("first_name"));

    executionRoot.execute(false).test().assertNoErrors().assertValue(expected);
  }

  @Test
  public void tableNameWhenParent() {
    ExecutionRoot executionRoot = new ExecutionRoot(tableName, tableAlias, null);
    assertEquals(tableName, executionRoot.tableNameWhenParent());
  }

  @Test
  public void getQueriedTables() {
    ExecutionRoot executionRoot = new ExecutionRoot(tableName, tableAlias, null);
    Set<String> queriedTables = executionRoot.getQueriedTables();
    assertEquals(queriedTables, Set.of(tableName));
  }
}
