/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;

/**
 * Class to construct and store GraphQL arguments from simple query components.
 *
 * @author Mingyi Zhang
 */
public class SQLArguments {

  private final Integer limit;
  private final Integer offset;
  private final JsonArray orderBy;
  private final JsonObject where;

  public SQLArguments(Map<String, Object> args) {
    this.limit = (Integer) args.get("limit");
    this.offset = (Integer) args.get("offset");
    this.orderBy =
        args.get("order_by") == null ? null : new JsonArray((List<?>) args.get("order_by"));
    this.where = JsonObject.mapFrom(args.get("where"));
  }

  public Integer getLimit() {
    return limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public JsonArray getOrderBy() {
    return orderBy;
  }

  public JsonObject getWhere() {
    return where;
  }
}
