/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This interface is used by top-level component which executes SQL query.
 *
 * @author Kamil Bobrowski
 */
public interface ComponentExecutable extends ComponentParent {

  /**
   * Executes SQL query.
   *
   * @return {@link Single} which emits list of tables
   */
  Single<List<Map<String, Object>>> execute(
      SQLExecutor sqlExecutor, boolean lockTables, Consumer<SQLQuery> sqlQueryModifier);
}
