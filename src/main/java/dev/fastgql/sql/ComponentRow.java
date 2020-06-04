/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.sql;

import io.reactivex.Single;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

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
  public void setSqlExecutor(Function<String, Single<List<Map<String, Object>>>> sqlExecutor) {

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
  public Set<String> getQueriedTables() {
    return Set.of();
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
