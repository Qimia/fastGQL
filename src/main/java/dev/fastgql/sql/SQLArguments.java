/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Map;

/**
 * Class to construct and store GraphQL arguments from simple query components.
 *
 * @author Mingyi Zhang
 */
public class SQLArguments {

  private final Integer limit;
  private final Integer offset;
  private final JsonElement orderBy;
  private final JsonElement where;

  public SQLArguments(Map<String, Object> args) {
    this.limit = (Integer) args.get("limit");
    this.offset = (Integer) args.get("offset");
    Gson gson = new Gson();
    this.orderBy = gson.toJsonTree(args.get("order_by"));
    this.where = gson.toJsonTree(args.get("where"));
  }

  public Integer getLimit() {
    return limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public JsonElement getWhere() {
    return where;
  }

  public JsonElement getOrderBy() {
    return orderBy;
  }
}
