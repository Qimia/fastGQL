/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.fastgql.graphql.ConditionalOperatorTypes;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class of utility functions for constructing constraint queries.
 *
 * @author Mingyi Zhang
 */
public class SQLUtils {

  public static boolean isReferencingName(String name) {
    return name.endsWith("_ref");
  }

  public static String createOrderByQuery(
      JsonArray array, String aliasName, Map<String, String> tableFieldToAlias) {
    List<String> orderByQueryList = new ArrayList<>();
    for (int i = 0; i < array.size(); i++) {
      JsonObject object = array.get(i).getAsJsonObject();
      addOrderByQueryList(orderByQueryList, object, aliasName, tableFieldToAlias);
    }
    return String.join(", ", orderByQueryList);
  }

  private static void addOrderByQueryList(
      List<String> queryList,
      JsonObject object,
      String aliasName,
      Map<String, String> tableFieldToAlias) {
    for (String key : object.keySet()) {
      JsonElement value = object.get(key);
      if (value.isJsonObject()) {
        if (tableFieldToAlias.containsKey(key)) {
          addOrderByQueryList(
              queryList, value.getAsJsonObject(), tableFieldToAlias.get(key), tableFieldToAlias);
        }
      } else {
        queryList.add(String.format("%s.%s %s", aliasName, key, value.getAsString()));
      }
    }
  }

  public static String createBoolQuery(
      JsonObject obj, String aliasName, Map<String, String> tableFieldToAlias) {
    if (obj.size() == 0) {
      return "TRUE";
    }
    List<String> queryList =
        obj.keySet().stream()
            .map(
                key -> {
                  String query;
                  if (key.equals("_and")) {
                    query =
                        getArrayQuery(
                            obj.get(key).getAsJsonArray(), " AND ", aliasName, tableFieldToAlias);
                  } else if (key.equals("_or")) {
                    query =
                        getArrayQuery(
                            obj.get(key).getAsJsonArray(), " OR ", aliasName, tableFieldToAlias);
                  } else if (key.equals("_not")) {
                    query =
                        getNotQuery(obj.get(key).getAsJsonObject(), aliasName, tableFieldToAlias);
                  } else if (isReferencingName(key)) {
                    if (tableFieldToAlias.containsKey(key)) {
                      query =
                          createBoolQuery(
                              obj.get(key).getAsJsonObject(),
                              tableFieldToAlias.get(key),
                              tableFieldToAlias);
                    } else {
                      query = "";
                    }
                  } else {
                    query = getComparisonQuery(obj.get(key).getAsJsonObject(), aliasName, key);
                  }
                  return String.format("(%s)", query);
                })
            .collect(Collectors.toList());
    return String.join(" AND ", queryList);
  }

  private static String getArrayQuery(
      JsonArray array, String delimiter, String aliasName, Map<String, String> tableFieldToAlias) {
    List<String> queryList = new ArrayList<>();
    for (int i = 0; i < array.size(); i++) {
      queryList.add(
          String.format(
              "(%s)",
              createBoolQuery(array.get(i).getAsJsonObject(), aliasName, tableFieldToAlias)));
    }
    return String.join(delimiter, queryList);
  }

  private static String getNotQuery(
      JsonObject object, String aliasName, Map<String, String> tableFieldToAlias) {
    return String.format("NOT (%s)", createBoolQuery(object, aliasName, tableFieldToAlias));
  }

  private static String getComparisonQuery(JsonObject object, String aliasName, String key) {
    if (object.size() == 0) {
      return "TRUE";
    }
    List<String> queryList =
        object.keySet().stream()
            .map(
                opName -> {
                  String operator =
                      ConditionalOperatorTypes.getOperatorNameToValueMap().get(opName);
                  String compareValue = getCompareValue(object.get(opName));
                  return String.format("(%s.%s %s %s)", aliasName, key, operator, compareValue);
                })
            .collect(Collectors.toList());
    return String.join(" AND ", queryList);
  }

  private static String getCompareValue(JsonElement element) {
    if (element.isJsonPrimitive()) {
      return getPrimitiveString(element.getAsJsonPrimitive());
    } else if (element.isJsonArray()) {
      List<String> queryList = new ArrayList<>();
      element
          .getAsJsonArray()
          .forEach(
              e -> {
                if (e.isJsonPrimitive()) {
                  queryList.add(getPrimitiveString(e.getAsJsonPrimitive()));
                }
              });
      return String.format("(%s)", String.join(", ", queryList));
    } else {
      return "";
    }
  }

  private static String getPrimitiveString(JsonPrimitive json) {
    if (json.isNumber()) {
      return json.getAsString();
    } else {
      return String.format("'%s'", json.getAsString());
    }
  }

  public static List<Map<String, Object>> rowSetToList(RowSet<Row> rowSet) {
    List<String> columnNames = rowSet.columnsNames();
    List<Map<String, Object>> retList = new ArrayList<>();
    rowSet.forEach(
        row -> {
          Map<String, Object> r = new HashMap<>();
          columnNames.forEach(columnName -> r.put(columnName, row.getValue(columnName)));
          retList.add(r);
        });
    return retList;
  }
}
