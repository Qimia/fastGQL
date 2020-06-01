package ai.qimia.fastgql.schema.sql;

import java.util.Map;
import java.util.Objects;

public class ComponentRow implements Component {
  private final String table;
  private final String key;

  public ComponentRow(String table, String key) {
    this.table = table;
    this.key = key;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addKey(String.format("%s.%s", table, key));
  }

  @Override
  public Map<String, Object> extractValues(Map<String, Object> row) {
    return Map.of(key, row.get(String.format("%s_%s", table, key)));
  }
}
