package ai.qimia.fastgql.schema.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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

    ExecutionRoot executionRoot = new ExecutionRoot("customers", "customers", "c");
    executionRoot.setForgedResponse(forged);
    executionRoot.addComponent(new ComponentRow( "id"));
    executionRoot.addComponent(new ComponentRow("first_name"));

    ComponentReferencing addressRef = new ComponentReferencing("address_ref", "address", "addresses", "a", "id");
    addressRef.addComponent(new ComponentRow( "id"));
    addressRef.addComponent(new ComponentRow("street"));

    ComponentReferencing vehiclesRef = new ComponentReferencing("vehicles_ref", "vehicle", "vehicles", "v", "id");
    vehiclesRef.addComponent(new ComponentRow("id"));
    vehiclesRef.addComponent(new ComponentRow("model"));

    addressRef.addComponent(vehiclesRef);
    executionRoot.addComponent(addressRef);

    ComponentReferenced vehiclesOnCustomer = new ComponentReferenced("vehicles_on_customer", "id", "vehicles", "v", "customer");
    vehiclesOnCustomer.setForgedResponse(forged2);
    vehiclesOnCustomer.addComponent(new ComponentRow("model"));
    vehiclesOnCustomer.addComponent(new ComponentRow("year"));

    executionRoot.addComponent(vehiclesOnCustomer);
    System.out.println(executionRoot.execute());
  }

  public ExecutionRoot(String field, String table, String alias) {
    this.field = field;
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
    List<Map<String, Object>> response = forgedResponse.stream().map(
      row -> SQLResponseProcessor.constructResponse(row, components)).collect(Collectors.toList()
    );
    query.reset();
    return Map.of(field, response);
  }

  public void addComponent(Component component) {
    component.setTable(alias);
    components.add(component);
  }

  protected void modifyQuery(Consumer<SQLQuery> modifier) {
    modifier.accept(query);
  }
}
