/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class to handle part of SQL query related to querying another table which is referenced by key in
 * a current table. It is using LEFT JOIN to get this information.
 *
 * @author Kamil Bobrowski
 */
public class ComponentReferencing implements Component {
  private final String field;
  private String table;
  private final String key;
  private final String foreignTable;
  private final String foreignTableAlias;
  private final String foreignKey;
  private List<Component> components;
  private Set<String> queriedTables = new HashSet<>();
  private SQLExecutor sqlExecutor;

  /**
   * Construct component by providing information about key which is referencing foreign key and
   * foreign key which is referenced.
   *
   * @param field name of GraphQL field (e.g. address_ref)
   * @param key name of key which is referencing foreign key
   * @param foreignTable name of referenced foreign table
   * @param foreignTableAlias alias of referenced foreign table
   * @param foreignKey name of referenced foreign key
   */
  public ComponentReferencing(
      String field, String key, String foreignTable, String foreignTableAlias, String foreignKey) {
    this.field = field;
    this.key = key;
    this.foreignTable = foreignTable;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKey = foreignKey;
    this.components = new ArrayList<>();
    this.queriedTables.add(foreignTable);
  }

  @Override
  public void addComponent(Component component) {
    component.setTable(foreignTableAlias);
    component.setSqlExecutor(sqlExecutor);
    components.add(component);
    queriedTables.addAll(component.getQueriedTables());
  }

  @Override
  public String trueTableNameWhenParent() {
    return foreignTable;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addKey(table, key);
    query.addJoin(table, key, foreignTable, foreignTableAlias, foreignKey);
    components.forEach(component -> component.updateQuery(query));
  }

  @Override
  public void setTable(String table) {
    this.table = table;
  }

  @Override
  public void setSqlExecutor(SQLExecutor sqlExecutor) {
    this.sqlExecutor = sqlExecutor;
  }

  @Override
  public Single<Map<String, Object>> extractValues(Map<String, Object> row) {
    if (SQLResponseUtils.getValue(row, table, key) == null) {
      return Single.just(Map.of());
    }
    return SQLResponseUtils.constructResponse(row, components)
        .map(response -> Map.of(field, response));
  }

  @Override
  public Set<String> getQueriedTables() {
    return queriedTables;
  }
}
