/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.common.TableWithAlias;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
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
  private final SQLArguments sqlArguments;
  private final List<Component> components;
  private final Set<TableWithAlias> queriedTables = new HashSet<>();
  private final Function<Set<TableWithAlias>, String> lockQueryFunction;
  private final String unlockQuery;

  /**
   * Construct execution root by providing table name and its alias.
   *
   * @param tableName name of the table
   * @param tableAlias alias of the table
   * @param args argument from GraphQL query
   */
  public ExecutionRoot(
      String tableName,
      String tableAlias,
      SQLArguments args,
      Function<Set<TableWithAlias>, String> lockQueryFunction,
      String unlockQuery) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
    this.sqlArguments = args;
    this.components = new ArrayList<>();
    this.queriedTables.add(new TableWithAlias(tableName, tableAlias));
    this.lockQueryFunction = lockQueryFunction;
    this.unlockQuery = unlockQuery;
  }

  public String createQueryString(Consumer<SQLQuery> sqlQueryModifier) {
    SQLQuery sqlQuery = new SQLQuery(tableName, tableAlias, sqlArguments);
    components.forEach(component -> component.updateQuery(sqlQuery));
    if (sqlQueryModifier != null) {
      sqlQueryModifier.accept(sqlQuery);
    }
    return sqlQuery.build();
  }

  @Override
  public Single<List<Map<String, Object>>> execute(
      SQLExecutor sqlExecutor, boolean lockTables, Consumer<SQLQuery> sqlQueryModifier) {
    String queryString = createQueryString(sqlQueryModifier);
    log.debug("executing query: {}", queryString);

    Single<List<Map<String, Object>>> querySingle =
        sqlExecutor
            .execute(queryString)
            .flatMap(
                rowList -> {
                  if (rowList.size() > 0) {
                    List<Single<Map<String, Object>>> componentResponsesSingles =
                        rowList.stream()
                            .map(
                                row ->
                                    SQLResponseUtils.constructResponse(
                                        sqlExecutor, row, components))
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

    if (lockTables && lockQueryFunction != null) {
      return sqlExecutor
          .execute(lockQueryFunction.apply(getQueriedTables()))
          .flatMap(
              lockResult ->
                  querySingle.flatMap(
                      result -> {
                        if (unlockQuery != null && unlockQuery.length() > 0) {
                          return sqlExecutor.execute(unlockQuery).map(unlockResult -> result);
                        } else {
                          return Single.just(result);
                        }
                      }));
    } else {
      return querySingle;
    }
  }

  @Override
  public void addComponent(Component component) {
    component.setParentTableAlias(tableAlias);
    components.add(component);
    queriedTables.addAll(component.getQueriedTables());
  }

  @Override
  public String tableNameWhenParent() {
    return tableName;
  }

  @Override
  public Set<TableWithAlias> getQueriedTables() {
    return queriedTables;
  }
}
