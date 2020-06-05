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
  private SQLExecutor sqlExecutor;
  private SQLQuery query;
  private List<Component> components;
  private Set<String> queriedTables = new HashSet<>();

  /**
   * just for quick testing, will be moved to tests.
   *
   * @param args args
   */
  public static void main(String[] args) {

    final List<Map<String, Object>> forged =
        List.of(
            Map.of(
                "a0_id",
                101,
                "a0_first_name",
                "John",
                "a1_id",
                101,
                "a1_street",
                "StreetA",
                "a2_id",
                101,
                "a2_model",
                "Subaru"),
            Map.of(
                "a0_id",
                102,
                "a0_first_name",
                "Mike",
                "a1_id",
                102,
                "a1_street",
                "StreetB",
                "a2_id",
                102,
                "a2_model",
                "Ford"));

    final List<Map<String, Object>> forged2 =
        List.of(
            Map.of("a3_model", "Subaru", "a3_year", 2010, "a4_id", 101, "a4_first_name", "Klaus"),
            Map.of("a3_model", "BMW", "a3_year", 2012, "a4_id", 102, "a4_first_name", "John"));

    AliasGenerator aliasGenerator = new AliasGenerator();

    ComponentExecutable executionRoot = new ExecutionRoot("customers", aliasGenerator.getAlias());
    executionRoot.setSqlExecutor(new SQLExecutor(query -> Single.just(forged)));
    executionRoot.addComponent(new ComponentRow("id"));
    executionRoot.addComponent(new ComponentRow("first_name"));

    Component addressRef =
        new ComponentReferencing(
            "address_ref", "address", "addresses", aliasGenerator.getAlias(), "id");
    addressRef.addComponent(new ComponentRow("id"));
    addressRef.addComponent(new ComponentRow("street"));

    Component vehiclesRef =
        new ComponentReferencing(
            "vehicles_ref", "vehicle", "vehicles", aliasGenerator.getAlias(), "id");
    vehiclesRef.addComponent(new ComponentRow("id"));
    vehiclesRef.addComponent(new ComponentRow("model"));

    addressRef.addComponent(vehiclesRef);
    executionRoot.addComponent(addressRef);

    Component vehiclesOnCustomer =
        new ComponentReferenced(
            "vehicles_on_customer", "id", "vehicles", aliasGenerator.getAlias(), "customer");
    vehiclesOnCustomer.addComponent(new ComponentRow("model"));
    vehiclesOnCustomer.addComponent(new ComponentRow("year"));

    Component customerRef =
        new ComponentReferencing(
            "customer_ref", "customer", "customers", aliasGenerator.getAlias(), "id");
    customerRef.addComponent(new ComponentRow("id"));
    customerRef.addComponent(new ComponentRow("first_name"));
    vehiclesOnCustomer.addComponent(customerRef);

    executionRoot.addComponent(vehiclesOnCustomer);
    vehiclesOnCustomer.setSqlExecutor(new SQLExecutor(query -> Single.just(forged2)));
    System.out.println(executionRoot.getQueriedTables());
    System.out.println(executionRoot.execute().blockingGet());
  }

  /**
   * Construct execution root by providing table name and its alias.
   *
   * @param tableName name of the table
   * @param tableAlias alias of the table
   */
  public ExecutionRoot(String tableName, String tableAlias) {
    this.tableName = tableName;
    this.tableAlias = tableAlias;
    this.query = new SQLQuery(tableName, tableAlias);
    this.components = new ArrayList<>();
    this.queriedTables.add(tableName);
    this.sqlExecutor = new SQLExecutor();
  }

  @Override
  public Single<List<Map<String, Object>>> execute() {
    components.forEach(component -> component.updateQuery(query));
    String queryString = query.build();
    log.debug("executing query: {}", queryString);
    query.reset();
    return sqlExecutor
        .getSqlExecutorFunction()
        .apply(queryString)
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
