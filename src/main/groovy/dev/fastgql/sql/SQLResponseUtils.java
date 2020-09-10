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

/**
 * Helper functions to extract values from SQL response.
 *
 * @author Kamil Bobrowski
 */
public class SQLResponseUtils {

  /**
   * Get value for single key from response row.
   *
   * @param row response row
   * @param tableAlias alias of a table
   * @param tableKeyName key name in a table
   * @return value for given alias and key
   */
  public static Object getValue(Map<String, Object> row, String tableAlias, String tableKeyName) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(tableAlias);
    Objects.requireNonNull(tableKeyName);
    return row.get(String.format("%s_%s", tableAlias, tableKeyName));
  }

  /**
   * Build formatted GraphQL response from given SQL response row and list of {@link Component}. It
   * collects all {@link Single} returned from extractValues method of each component and then
   * combines them in a single Map.
   *
   * @param row SQL response row
   * @param components list of components which can extract values from this row
   * @return {@link Single} which emits combined Map of all responses emitted by input components
   */
  public static Single<Map<String, Object>> constructResponse(
      SQLExecutor sqlExecutor, Map<String, Object> row, List<Component> components) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(components);
    List<Single<Map<String, Object>>> componentValuesSingles =
        components.stream()
            .map(component -> component.extractValues(sqlExecutor, row))
            .collect(Collectors.toList());
    return Single.zip(
        componentValuesSingles,
        componentValuesObjects -> {
          Map<String, Object> r = new HashMap<>();
          Arrays.stream(componentValuesObjects)
              .map(
                  componentValueObject -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> componentValue = (Map<String, Object>) componentValueObject;
                    return componentValue;
                  })
              .forEach(r::putAll);
          return r;
        });
  }
}
