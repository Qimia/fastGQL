/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.common.TableWithAlias;
import io.reactivex.Single;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class to handle part of SQL query related to extracting value of single key.
 *
 * @author Kamil Bobrowski
 */
public class ComponentRow implements Component {
  private String parentTableAlias;
  private final String keyName;

  /**
   * Construct new component by providing key name to be queried.
   *
   * @param keyName key name
   */
  public ComponentRow(String keyName) {
    this.keyName = keyName;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addKey(parentTableAlias, keyName);
  }

  @Override
  public void setParentTableAlias(String parentTableAlias) {
    this.parentTableAlias = parentTableAlias;
  }

  @Override
  public Single<Map<String, Object>> extractValues(
      SQLExecutor sqlExecutor, Map<String, Object> row) {
    Object value = SQLResponseUtils.getValue(row, parentTableAlias, keyName);
    if (value == null) {
      return Single.just(Map.of());
    }
    return Single.just(Map.of(keyName, value));
  }

  @Override
  public Set<TableWithAlias> getQueriedTables() {
    return Set.of();
  }

  @Override
  public void addComponent(Component component) {
    throw new RuntimeException("ComponentRow cannot have any child components");
  }

  @Override
  public String tableNameWhenParent() {
    throw new RuntimeException("ComponentRow cannot have any child components");
  }
}
