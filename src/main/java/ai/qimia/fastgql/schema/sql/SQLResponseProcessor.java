package ai.qimia.fastgql.schema.sql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLResponseProcessor {
  public static Object getValue(Map<String, Object> row, String alias, String key) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(alias);
    Objects.requireNonNull(key);
    return row.get(String.format("%s_%s", alias, key));
  }

  public static Map<String, Object> constructResponse(Map<String, Object> row, List<Component> components) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(components);
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
  }
}
