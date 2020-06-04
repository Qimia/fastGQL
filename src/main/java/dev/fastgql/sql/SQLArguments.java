/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import java.util.Map;

public class SQLArguments {
  private Integer limit;
  private Integer offset;

  public SQLArguments(Map<String, Object> args) {
    this.limit = (Integer) args.get("limit");
    this.offset = (Integer) args.get("offset");
  }

  public Integer getLimit() {
    return limit;
  }

  public String getLimitQuery() {
    if (limit == null) {
      return "";
    }
    return String.format(
        "LIMIT %d", limit
    );
  }

  public Integer getOffset() {
    return offset;
  }

  public String getOffsetQuery() {
    if (offset == null) {
      return "";
    }
    return String.format(
        "OFFSET %d", offset
    );
  }

  public Boolean isEmpty() {
    return this.limit == null && this.offset == null;
  }
}
