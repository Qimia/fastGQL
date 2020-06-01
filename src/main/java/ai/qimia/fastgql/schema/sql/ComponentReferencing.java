package ai.qimia.fastgql.schema.sql;

import java.util.*;

public class ComponentReferencing implements Component {
  private final String field;
  private String table;
  private final String key;
  private final String foreignTable;
  private final String foreignTableAlias;
  private final String foreignKey;
  private List<Component> components;

  public ComponentReferencing(String field, String key, String foreignTable, String foreignTableAlias, String foreignKey) {
    this.field = field;
    this.key = key;
    this.foreignTable = foreignTable;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKey = foreignKey;
    this.components = new ArrayList<>();
  }

  @Override
  public void addComponent(Component component) {
    component.setTable(foreignTableAlias);
    components.add(component);
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addJoin(table, key, foreignTable, foreignTableAlias, foreignKey);
    components.forEach(component -> component.updateQuery(query));
  }

  @Override
  public void setTable(String table) {
    this.table = table;
  }

  @Override
  public Map<String, Object> extractValues(Map<String, Object> row) {
    return Map.of(field, SQLResponseProcessor.constructResponse(row, components));
  }
}
