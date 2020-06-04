/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class SQLResponseUtils {
  public static Object getValue(Map<String, Object> row, String alias, String key) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(alias);
    Objects.requireNonNull(key);
    return row.get(String.format("%s_%s", alias, key));
  }

  public static Single<Map<String, Object>> constructResponse(
      Map<String, Object> row, List<Component> components) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(components);
    List<Single<Map<String, Object>>> observables =
        components.stream()
            .map(component -> component.extractValues(row))
            .collect(Collectors.toList());
    return Single.zip(
        observables,
        values -> {
          Map<String, Object> r = new HashMap<>();
          Arrays.stream(values).map(value -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> retValue = (Map<String, Object>) value;
            return retValue;
          }).forEach(r::putAll);
          return r;
        });
  }
}
