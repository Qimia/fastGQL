/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.Map;

/**
 * This interface is used by each child component.
 *
 * @author Kamil Bobrowski
 */
public interface Component extends ComponentParent {

  /**
   * Updates query to account for information this component is responsible for.
   *
   * @param query {@link SQLQuery} object to be updated
   */
  void updateQuery(SQLQuery query);

  /**
   * Sets parent table alias.
   *
   * @param parentTableAlias alias of parent table
   */
  void setParentTableAlias(String parentTableAlias);

  /**
   * Extract values which are handled by this component from given SQL response row.
   *
   * @param row SQL response row
   * @return {@link Single} which emits representation of data extracted from SQL response row
   */
  Single<Map<String, Object>> extractValues(SQLExecutor sqlExecutor, Map<String, Object> row);
}
