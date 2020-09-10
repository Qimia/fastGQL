/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.fastgql.common.TableWithAlias;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ExecutionRootTest {

  static final String tableName = "testTableName";
  static final String tableAlias = "testTableAlias";

  @Test
  public void execute() {
    ExecutionRoot executionRoot = new ExecutionRoot(tableName, tableAlias, null, null, null);
    List<Map<String, Object>> forged =
        List.of(
            Map.of("testTableAlias_id", 101, "testTableAlias_first_name", "John"),
            Map.of("testTableAlias_id", 102, "testTableAlias_first_name", "Mike"));
    List<Map<String, Object>> expected =
        List.of(Map.of("id", 101, "first_name", "John"), Map.of("id", 102, "first_name", "Mike"));

    executionRoot.addComponent(new ComponentRow("id"));
    executionRoot.addComponent(new ComponentRow("first_name"));

    executionRoot
        .execute(query -> Single.just(forged), false, null)
        .test()
        .assertNoErrors()
        .assertValue(expected);
  }

  @Test
  public void tableNameWhenParent() {
    ExecutionRoot executionRoot = new ExecutionRoot(tableName, tableAlias, null, null, null);
    assertEquals(tableName, executionRoot.tableNameWhenParent());
  }

  @Test
  public void getQueriedTables() {
    ExecutionRoot executionRoot = new ExecutionRoot(tableName, tableAlias, null, null, null);
    Set<TableWithAlias> queriedTables = executionRoot.getQueriedTables();
    Set<String> queriedTablesString =
        queriedTables.stream().map(TableWithAlias::toString).collect(Collectors.toSet());
    Set<String> expected = Set.of((new TableWithAlias(tableName, tableAlias)).toString());
    assertEquals(expected, queriedTablesString);
  }
}
