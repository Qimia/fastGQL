package ai.qimia.fastgql.schema.sql;

import java.util.*;
import java.util.stream.Collectors;

public class ComponentReferencing implements Component {
  private final String field;
  private final String table;
  private final String key;
  private final String foreignTable;
  private final String foreignTableAlias;
  private final String foreignKey;
  private List<Component> components;

  public ComponentReferencing(String field, String table, String key, String foreignTable, String foreignTableAlias, String foreignKey) {
    this.field = field;
    this.table = table;
    this.key = key;
    this.foreignTable = foreignTable;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKey = foreignKey;
    this.components = new ArrayList<>();
  }

  public void addComponent(Component component) {
    components.add(component);
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addJoin(table, key, foreignTable, foreignTableAlias, foreignKey);
    components.forEach(component -> component.updateQuery(query));
  }

  @Override
  public Map<String, Object> extractValues(Map<String, Object> row) {
    Map<String, Object> ret = new HashMap<>();
    components
      .stream()
      .map(component -> component.extractValues(row))
      .forEach(map -> ret.putAll(
        map.entrySet().stream().collect(
          Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        )
      ));
    return Map.of(field, ret);
  }
}
