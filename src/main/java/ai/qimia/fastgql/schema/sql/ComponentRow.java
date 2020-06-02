package ai.qimia.fastgql.schema.sql;

import java.util.Map;
import java.util.Objects;

public class ComponentRow implements Component {
  private String table;
  private final String key;

  public ComponentRow(String key) {
    this.key = key;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addKey(String.format("%s.%s AS %s_%s", table, key, table, key));
  }

  @Override
  public void setTable(String table) {
    this.table = table;
  }

  @Override
  public Map<String, Object> extractValues(Map<String, Object> row) {
    return Map.of(key, SQLResponseProcessor.getValue(row, table, key));
  }

  @Override
  public void addComponent(Component component) {
    throw new RuntimeException("ComponentRow cannot have any child components");
  }

  @Override
  public String trueTableNameWhenParent() {
    throw new RuntimeException("ComponentRow cannot have any child components");
  }
}
