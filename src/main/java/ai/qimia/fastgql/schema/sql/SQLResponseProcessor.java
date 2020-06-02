package ai.qimia.fastgql.schema.sql;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Pool;

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
        .forEach(r::putAll);
      return r;
    });
  }

  public static Single<List<Map<String, Object>>> executeQuery(String query, Pool client) {
    return client.rxQuery(query).map(rowSet -> {
      List<String> columnNames = rowSet.columnsNames();
      List<Map<String, Object>> rList = new ArrayList<>();
      rowSet.forEach(row -> {
        Map<String, Object> r = new HashMap<>();
        columnNames.forEach(columnName -> r.put(columnName, row.getValue(columnName)));
        rList.add(r);
      });
      return rList;
    });
  }
}
