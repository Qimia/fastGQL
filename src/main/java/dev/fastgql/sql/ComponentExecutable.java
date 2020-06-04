/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.List;
import java.util.Map;

public interface ComponentExecutable extends ComponentParent {
  Single<List<Map<String, Object>>> execute();
}
