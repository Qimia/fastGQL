/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.sql;

import io.reactivex.Single;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public interface ComponentParent {
  void addComponent(Component component);

  String trueTableNameWhenParent();

  void setSqlExecutor(Function<String, Single<List<Map<String, Object>>>> sqlExecutor);

  Set<String> getQueriedTables();
}
