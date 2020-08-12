/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.common.TableWithAlias;
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
  private final String fieldName;
  private String parentTableAlias;
  private final String keyName;
  private final String foreignTableName;
  private final String foreignTableAlias;
  private final String foreignKeyName;
  private final List<Component> components;
  private final Set<TableWithAlias> queriedTables = new HashSet<>();

  /**
   * Construct component by providing information about key which is referencing foreign key and
   * foreign key which is referenced.
   *
   * @param fieldName name of GraphQL field (e.g. address_ref)
   * @param keyName name of key which is referencing foreign key
   * @param foreignTableName name of referenced foreign table
   * @param foreignTableAlias alias of referenced foreign table
   * @param foreignKeyName name of referenced foreign key
   */
  public ComponentReferencing(
      String fieldName,
      String keyName,
      String foreignTableName,
      String foreignTableAlias,
      String foreignKeyName) {
    this.fieldName = fieldName;
    this.keyName = keyName;
    this.foreignTableName = foreignTableName;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKeyName = foreignKeyName;
    this.components = new ArrayList<>();
    this.queriedTables.add(new TableWithAlias(foreignTableName, foreignTableAlias));
  }

  @Override
  public void addComponent(Component component) {
    component.setParentTableAlias(foreignTableAlias);
    // component.setSqlExecutor(sqlExecutor);
    components.add(component);
    queriedTables.addAll(component.getQueriedTables());
  }

  @Override
  public String tableNameWhenParent() {
    return foreignTableName;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    Objects.requireNonNull(query);
    query.addKey(parentTableAlias, keyName);
    query.addJoin(parentTableAlias, keyName, foreignTableName, foreignTableAlias, foreignKeyName);
    query.addFieldToAlias(fieldName, foreignTableAlias);
    components.forEach(component -> component.updateQuery(query));
  }

  @Override
  public void setParentTableAlias(String parentTableAlias) {
    this.parentTableAlias = parentTableAlias;
  }

  @Override
  public Single<Map<String, Object>> extractValues(
      SQLExecutor sqlExecutor, Map<String, Object> row) {
    if (SQLResponseUtils.getValue(row, parentTableAlias, keyName) == null) {
      return Single.just(Map.of());
    }
    return SQLResponseUtils.constructResponse(sqlExecutor, row, components)
        .map(response -> Map.of(fieldName, response));
  }

  @Override
  public Set<TableWithAlias> getQueriedTables() {
    return queriedTables;
  }
}
