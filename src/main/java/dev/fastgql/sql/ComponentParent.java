/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import java.util.Set;

public interface ComponentParent {
  void addComponent(Component component);

  String trueTableNameWhenParent();

  void setSqlExecutor(SqlExecutor sqlExecutor);

  Set<String> getQueriedTables();
}
