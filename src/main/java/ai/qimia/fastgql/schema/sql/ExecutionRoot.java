package ai.qimia.fastgql.schema.sql;

import ai.qimia.fastgql.schema.FieldType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExecutionRoot {
  private final String field;
  private final String alias;
  private SQLQuery query;
  private List<Component> components;
  private List<Map<String, Object>> forgedResponse;

  public static void main(String[] args) {

    List<Map<String, Object>> forged = List.of(
      Map.of(
        "c_id", 101,
        "c_first_name", "John",
        "a_id", 101,
        "a_street", "StreetA",
        "v_id", 101,
        "v_model", "Subaru"
      ),
      Map.of(
        "c_id", 102,
        "c_first_name", "Mike",
        "a_id", 102,
        "a_street", "StreetB",
        "v_id", 102,
        "v_model", "Ford"
      )
    );

    List<Map<String, Object>> forged2 = List.of(
      Map.of(
        "v_model", "Subaru",
        "v_year", 2010
      ),
      Map.of(
        "v_model", "BMW",
        "v_year", 2012
      )
    );

    ExecutionRoot executionRoot = new ExecutionRoot("customers", "c");
    executionRoot.setForgedResponse(forged);
    executionRoot.addComponent(new ComponentRow( "c", "id"));
    executionRoot.addComponent(new ComponentRow("c", "first_name"));

    ComponentReferencing addressRef = new ComponentReferencing("address_ref", "c", "address", "addresses", "a", "id");
    addressRef.addComponent(new ComponentRow("a", "id"));
    addressRef.addComponent(new ComponentRow("a", "street"));

    ComponentReferencing vehiclesRef = new ComponentReferencing("vehicles_ref", "a", "vehicle", "vehicles", "v", "id");
    vehiclesRef.addComponent(new ComponentRow("v", "id"));
    vehiclesRef.addComponent(new ComponentRow("v", "model"));

    addressRef.addComponent(vehiclesRef);
    executionRoot.addComponent(addressRef);

    ComponentReferenced vehiclesOnCustomer = new ComponentReferenced("vehicles_on_customer", "c", "id", "vehicles", "v", "customer");
    vehiclesOnCustomer.setForgedResponse(forged2);
    vehiclesOnCustomer.addComponent(new ComponentRow("v", "model"));
    vehiclesOnCustomer.addComponent(new ComponentRow("v", "year"));

    executionRoot.addComponent(vehiclesOnCustomer);
    System.out.println(executionRoot.execute());
  }

  public ExecutionRoot(String table, String alias) {
    this.field = table;
    this.alias = alias;
    this.query = new SQLQuery(table, alias);
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
