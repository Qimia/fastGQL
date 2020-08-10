/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to handle execution of SQL query. It contains {@link SQLQuery} which can be altered by each
 * of child {@link Component} and {@link SQLExecutor} which is used to execute this query. It also
 * tracks which tables need to be queried (this information is used by subscription data fetcher).
 *
 * @author Kamil Bobrowski
 */
public class ExecutionRoot implements ComponentExecutable {
  private final Logger log = LoggerFactory.getLogger(ExecutionRoot.class);
  private final String tableName;
  private final String tableAlias;
  private final SQLQuery query;
  private final List<Component> components;
  private final Set<String> queriedTables = new HashSet<>();
  private SQLExecutor sqlExecutor;

  /**
   * Construct execution root by providing table name and its alias.
   *
   * @param tableName name of the table
   * @param tableAlias alias of the table
   * @param args argument from GraphQL query
   */
  public ExecutionRoot(String tableName, String tableAlias, SQLArguments args) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
    this.query = new SQLQuery(tableName, tableAlias, args);
    this.components = new ArrayList<>();
    this.queriedTables.add(tableName);
  }

  @Override
  public Single<List<Map<String, Object>>> execute(boolean lockTables) {
    if (sqlExecutor == null) {
      throw new RuntimeException("SQLExecutor not initialized");
    }

    String lockTablesQueryString =
        String.format("LOCK TABLE %s", String.join(", ", getQueriedTables()));

    components.forEach(component -> component.updateQuery(query));
    String queryString = query.build();
    log.debug("executing query: {}", queryString);
    query.reset();

    Single<List<Map<String, Object>>> querySingle =
        sqlExecutor
            .execute(queryString)
            .flatMap(
                rowList -> {
                  if (rowList.size() > 0) {
                    List<Single<Map<String, Object>>> componentResponsesSingles =
                        rowList.stream()
                            .map(row -> SQLResponseUtils.constructResponse(row, components))
                            .collect(Collectors.toList());
                    return Single.zip(
                        componentResponsesSingles,
                        componentResponsesObjects ->
                            Arrays.stream(componentResponsesObjects)
                                .map(
                                    componentResponseObject -> {
                                      @SuppressWarnings("unchecked")
                                      Map<String, Object> componentResponse =
                                          (Map<String, Object>) componentResponseObject;
                                      return componentResponse;
                                    })
                                .collect(Collectors.toList()));
                  }
                  return Single.just(List.of());
                });

    if (lockTables) {
      return sqlExecutor.execute(lockTablesQueryString).flatMap(result -> querySingle);
    } else {
      return querySingle;
    }
  }

  @Override
  public void addComponent(Component component) {
    component.setParentTableAlias(tableAlias);
    component.setSqlExecutor(sqlExecutor);
    components.add(component);
    queriedTables.addAll(component.getQueriedTables());
  }

  @Override
  public String tableNameWhenParent() {
    return tableName;
  }

  @Override
  public void setSqlExecutor(SQLExecutor sqlExecutor) {
    this.sqlExecutor = sqlExecutor;
    this.components.forEach(component -> component.setSqlExecutor(sqlExecutor));
  }

  @Override
  public Set<String> getQueriedTables() {
    return queriedTables;
  }

  protected void modifyQuery(Consumer<SQLQuery> modifier) {
    modifier.accept(query);
  }
}
