package ai.qimia.fastgql.schema.sql;

import ai.qimia.fastgql.schema.FieldType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionRoot {
  private final String field;
  private SQLQuery query;
  private List<Component> components;
  private List<Map<String, Object>> forgedResponse;

  public static void main(String[] args) {

    List<Map<String, Object>> forged = List.of(
      Map.of(
        "customers_id", 101,
        "customers_first_name", "John",
        "addresses_id", 101,
        "addresses_street", "StreetA",
        "vehicles_id", 101,
        "vehicles_model", "Subaru"
      ),
      Map.of(
        "customers_id", 102,
        "customers_first_name", "Mike",
        "addresses_id", 102,
        "addresses_street", "StreetB",
        "vehicles_id", 102,
        "vehicles_model", "Ford"
      )
    );

    List<Map<String, Object>> forged2 = List.of(
      Map.of(
        "vehicles_model", "Subaru",
        "vehicles_year", 2010
      ),
      Map.of(
        "vehicles_model", "BMW",
        "vehicles_year", 2012
      )
    );

    ExecutionRoot executionRoot = new ExecutionRoot("customers");
    executionRoot.setForgedResponse(forged);
    executionRoot.addComponent(new ComponentRow( "customers", "id"));
    executionRoot.addComponent(new ComponentRow("customers", "first_name"));

    ComponentReferencing addressRef = new ComponentReferencing("address_ref", "customers", "address", "addresses", "id");
    addressRef.addComponent(new ComponentRow("addresses", "id"));
    addressRef.addComponent(new ComponentRow("addresses", "street"));

    ComponentReferencing vehiclesRef = new ComponentReferencing("vehicles_ref", "addresses", "vehicle", "vehicles", "id");
    vehiclesRef.addComponent(new ComponentRow("vehicles", "id"));
    vehiclesRef.addComponent(new ComponentRow("vehicles", "model"));

    addressRef.addComponent(vehiclesRef);
    executionRoot.addComponent(addressRef);

    ComponentReferenced vehiclesOnCustomer = new ComponentReferenced("vehicles_on_customer", "customers", "id", "vehicles", "customer");
    vehiclesOnCustomer.setForgedResponse(forged2);
    vehiclesOnCustomer.addComponent(new ComponentRow("vehicles", "model"));
    vehiclesOnCustomer.addComponent(new ComponentRow("vehicles", "year"));

    executionRoot.addComponent(vehiclesOnCustomer);
    System.out.println(executionRoot.execute());
  }

  public ExecutionRoot(String table) {
    this.field = table;
    this.query = new SQLQuery(table);
    this.components = new ArrayList<>();
  }

  public void setForgedResponse(List<Map<String, Object>> forgedResponse) {
    this.forgedResponse = forgedResponse;
  }

  public Map<String, Object> execute() {
    components.forEach(component -> component.updateQuery(query));
    System.out.println(query.build());
    List<Map<String, Object>> response = forgedResponse.stream().map(row -> {
      Map<String, Object> ret = new HashMap<>();
      components
        .stream()
        .map(component -> component.extractValues(row))
        .forEach(map -> ret.putAll(
          map.entrySet().stream().collect(
            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
          )
        ));
      return ret;
    }).collect(Collectors.toList());
    query.reset();
    return Map.of(field, response);
  }

  public void addComponent(Component component) {
    components.add(component);
  }

  protected SQLQuery getQuery() {
    return query;
  }
}
