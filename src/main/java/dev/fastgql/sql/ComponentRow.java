package dev.fastgql.sql;

import io.reactivex.Single;

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
    query.addKey(table, key);
  }

  @Override
  public void setTable(String table) {
    this.table = table;
  }

  @Override
  public Single<Map<String, Object>> extractValues(Map<String, Object> row) {
    Object value = SQLResponseUtils.getValue(row, table, key);
    if (value == null) {
      return Single.just(Map.of());
    }
    return Single.just(Map.of(key, value));
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
