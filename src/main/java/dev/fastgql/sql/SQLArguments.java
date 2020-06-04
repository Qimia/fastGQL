/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.fastgql.common.QualifiedName;
import java.util.LinkedHashMap;
import java.util.Map;

public class SQLArguments {

  private final String table;
  private final Integer limit;
  private final Integer offset;
  private final JsonElement orderBy;

  public SQLArguments(String table, Map<String, Object> args) {
    this.table = table;
    this.limit = (Integer) args.get("limit");
    this.offset = (Integer) args.get("offset");
    Gson gson = new Gson();
    this.orderBy = gson.toJsonTree(args.get("order_by"));
  }

  public Integer getLimit() {
    return limit;
  }

  public Integer getOffset() {
    return offset;
  }

  public LinkedHashMap<String, String> getQualifiedNameToOrderMap() {
    LinkedHashMap<String, String> qualifiedNameToOrder = new LinkedHashMap<>();
    if (!orderBy.isJsonArray()) {
      return qualifiedNameToOrder;
    }
    JsonArray orderByArray = orderBy.getAsJsonArray();
    for (int i = 0; i < orderByArray.size(); i++) {
      JsonObject object = orderByArray.get(i).getAsJsonObject();
      addQualifiedNameToOrder(table, object, qualifiedNameToOrder);
    }
    return qualifiedNameToOrder;
  }

  private void addQualifiedNameToOrder(
      String table, JsonObject object, Map<String, String> qualifiedNameToOrder) {
    for (String key : object.keySet()) {
      JsonElement value = object.get(key);
      if (value.isJsonObject()) {

        addQualifiedNameToOrder(key, value.getAsJsonObject(), qualifiedNameToOrder);
      } else {
        QualifiedName qualifiedName = new QualifiedName(table, key);
        qualifiedNameToOrder.put(qualifiedName.getQualifiedName(), value.getAsString());
      }
    }
  }
}
