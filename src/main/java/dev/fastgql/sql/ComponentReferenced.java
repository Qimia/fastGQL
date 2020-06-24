/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.Map;

/**
 * Class to handle part of SQL query related to querying list of other tables which are referencing
 * key in a current table. It is executing separate query to get this information.
 *
 * @author Kamil Bobrowski
 */
public class ComponentReferenced extends ExecutionRoot implements Component {
  private final String keyName;
  private final String fieldName;
  private final String foreignTableAlias;
  private final String foreignKeyName;
  private String parentTableAlias;

  /**
   * Construct component by providing information about key which is referenced by foreign key and
   * foreign key which is referencing this key.
   *
   * @param fieldName name of GraphQL field (e.g. customers_on_address)
   * @param keyName name of key which is referenced by foreign key
   * @param foreignTable name of foreign table which is referencing this table
   * @param foreignTableAlias alias of foreign table which is referencing this table
   * @param foreignKeyName foreign key which is referencing this key
   * @param args arguments parsed from GraphQL query
   */
  public ComponentReferenced(
      String fieldName,
      String keyName,
      String foreignTable,
      String foreignTableAlias,
      String foreignKeyName,
      SQLArguments args) {
    super(foreignTable, foreignTableAlias, args);
    this.keyName = keyName;
    this.fieldName = fieldName;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKeyName = foreignKeyName;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    query.addKey(parentTableAlias, keyName);
  }

  @Override
  public void setParentTableAlias(String parentTableAlias) {
    this.parentTableAlias = parentTableAlias;
  }

  @Override
  public Single<Map<String, Object>> extractValues(Map<String, Object> row) {
    Object keyValue = SQLResponseUtils.getValue(row, parentTableAlias, keyName);
    if (keyValue == null) {
      return Single.just(Map.of());
    }
    modifyQuery(
        query ->
            query.addWhereConditions(
                String.format(
                    "(%s.%s = %s)", foreignTableAlias, foreignKeyName, keyValue.toString())));
    return execute().map(response -> Map.of(fieldName, response));
  }
}
