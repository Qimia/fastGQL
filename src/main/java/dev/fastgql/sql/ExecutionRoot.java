/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExecutionRoot implements ComponentExecutable {
  private final String table;
  private final SQLArguments args;
  private final String alias;
  private final Function<String, Single<List<Map<String, Object>>>> sqlExecutor;
  private SQLQuery query;
  private List<Component> components;

  public static void main(String[] args) {

    List<Map<String, Object>> forged =
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

    List<Map<String, Object>> forged2 =
        List.of(
            Map.of("a3_model", "Subaru", "a3_year", 2010, "a4_id", 101, "a4_first_name", "Klaus"),
            Map.of("a3_model", "BMW", "a3_year", 2012, "a4_id", 102, "a4_first_name", "John"));

    AliasGenerator aliasGenerator = new AliasGenerator();
    SQLArguments myArgs =
        new SQLArguments(
            Map.of(
                "limit", 1,
                "offset", 1));

    ComponentExecutable executionRoot =
        new ExecutionRoot(
            "customers", aliasGenerator.getAlias(), myArgs, query -> Single.just(forged));
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
            "vehicles_on_customer",
            myArgs,
            "id",
            "vehicles",
            aliasGenerator.getAlias(),
            "customer",
            query -> Single.just(forged2));
    vehiclesOnCustomer.addComponent(new ComponentRow("model"));
    vehiclesOnCustomer.addComponent(new ComponentRow("year"));

    Component customerRef =
        new ComponentReferencing(
            "customer_ref", "customer", "customers", aliasGenerator.getAlias(), "id");
    customerRef.addComponent(new ComponentRow("id"));
    customerRef.addComponent(new ComponentRow("first_name"));
    vehiclesOnCustomer.addComponent(customerRef);

    executionRoot.addComponent(vehiclesOnCustomer);
    System.out.println(executionRoot.execute().blockingGet());
  }

  public ExecutionRoot(
      String table,
      String alias,
      SQLArguments args,
      Function<String, Single<List<Map<String, Object>>>> sqlExecutor) {
    this.table = table;
    this.args = args;
    this.alias = alias;
    this.query = new SQLQuery(table, alias, args);
    this.components = new ArrayList<>();
    this.sqlExecutor = sqlExecutor;
  }

  @Override
  public Single<List<Map<String, Object>>> execute() {
    components.forEach(component -> component.updateQuery(query));
    String queryString = query.build();
    System.out.println(queryString);
    query.reset();
    return sqlExecutor
        .apply(queryString)
        .flatMap(
            input -> {
              if (input.size() > 0) {
                List<Single<Map<String, Object>>> observables =
                    input.stream()
                        .map(row -> SQLResponseUtils.constructResponse(row, components))
                        .collect(Collectors.toList());
                return Single.zip(
                    observables,
                    values ->
                        Arrays.stream(values)
                            .map(value -> (Map<String, Object>) value)
                            .collect(Collectors.toList()));
              }
              return Single.just(List.of());
            });
  }

  @Override
  public void addComponent(Component component) {
    component.setTable(alias);
    components.add(component);
  }

  @Override
  public String trueTableNameWhenParent() {
    return table;
  }

  protected void modifyQuery(Consumer<SQLQuery> modifier) {
    modifier.accept(query);
  }
}
