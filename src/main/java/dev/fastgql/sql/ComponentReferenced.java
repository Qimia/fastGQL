/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.Map;

public class ComponentReferenced extends ExecutionRoot implements Component {
  private String table;
  private final String key;
  private final String field;
  private final String foreignTableAlias;
  private final String foreignKey;

  public ComponentReferenced(
      String field, String key, String foreignTable, String foreignTableAlias, String foreignKey) {
    super(foreignTable, foreignTableAlias);
    this.key = key;
    this.field = field;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKey = foreignKey;
  }

  @Override
  public void updateQuery(SQLQuery query) {
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
    modifyQuery(
        query ->
            query.addSuffix(
                String.format(
                    "WHERE %s.%s = %s", foreignTableAlias, foreignKey, value.toString())));
    return execute().map(response -> Map.of(field, response));
  }
}
