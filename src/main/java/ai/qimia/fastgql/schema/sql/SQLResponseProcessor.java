package ai.qimia.fastgql.schema.sql;

import io.reactivex.Single;

import java.util.*;
import java.util.stream.Collectors;

public class SQLResponseProcessor {
  public static Object getValue(Map<String, Object> row, String alias, String key) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(alias);
    Objects.requireNonNull(key);
    return row.get(String.format("%s_%s", alias, key));
  }

  public static Single<Map<String, Object>> constructResponse(Map<String, Object> row, List<Component> components) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(components);
    List<Single<Map<String, Object>>> observables = components
      .stream()
      .map(component -> component.extractValues(row))
      .collect(Collectors.toList());
    return Single.zip(observables, values -> {
      Map<String, Object> r = new HashMap<>();
      Arrays
        .stream(values)
        .map(value -> (Map<String, Object>) value)
        .forEach(map -> r.putAll(
           map.entrySet().stream().collect(
             Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
           )
      ));
      return r;
    });
  }
}
