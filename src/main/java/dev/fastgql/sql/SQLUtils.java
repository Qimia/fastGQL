/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.graphql.ConditionalOperatorTypes;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
      JsonObject object = array.getJsonObject(i);
      addOrderByQueryList(orderByQueryList, object, aliasName, tableFieldToAlias);
    }
    return String.join(", ", orderByQueryList);
  }

  private static void addOrderByQueryList(
      List<String> queryList,
      JsonObject object,
      String aliasName,
      Map<String, String> tableFieldToAlias) {
    for (String key : object.fieldNames()) {
      Object value = object.getValue(key);
      if (value instanceof JsonObject) {
        if (tableFieldToAlias.containsKey(key)) {
          addOrderByQueryList(
              queryList, JsonObject.mapFrom(value), tableFieldToAlias.get(key), tableFieldToAlias);
        }
      } else {
        queryList.add(String.format("%s.%s %s", aliasName, key, value));
      }
    }
  }

  public static String createBoolQuery(
      JsonObject obj, String aliasName, Map<String, String> tableFieldToAlias) {
    if (obj.size() == 0) {
      return "TRUE";
    }
    List<String> queryList =
        obj.fieldNames().stream()
            .map(
                key -> {
                  String query;
                  if (key.equals("_and")) {
                    query =
                        getArrayQuery(obj.getJsonArray(key), " AND ", aliasName, tableFieldToAlias);
                  } else if (key.equals("_or")) {
                    query =
                        getArrayQuery(obj.getJsonArray(key), " OR ", aliasName, tableFieldToAlias);
                  } else if (key.equals("_not")) {
                    query = getNotQuery(obj.getJsonObject(key), aliasName, tableFieldToAlias);
                  } else if (isReferencingName(key)) {
                    if (tableFieldToAlias.containsKey(key)) {
                      query =
                          createBoolQuery(
                              obj.getJsonObject(key),
                              tableFieldToAlias.get(key),
                              tableFieldToAlias);
                    } else {
                      query = "";
                    }
                  } else {
                    query = getComparisonQuery(obj.getJsonObject(key), aliasName, key);
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
              "(%s)", createBoolQuery(array.getJsonObject(i), aliasName, tableFieldToAlias)));
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
        object.fieldNames().stream()
            .map(
                opName -> {
                  String operator =
                      ConditionalOperatorTypes.getOperatorNameToValueMap().get(opName);
                  String compareValue = getCompareValue(object.getValue(opName));
                  return String.format("(%s.%s %s %s)", aliasName, key, operator, compareValue);
                })
            .collect(Collectors.toList());
    return String.join(" AND ", queryList);
  }

  private static String getCompareValue(Object object) {
    if (isPrimitive(object)) {
      return getPrimitiveString(object);
    } else if (object instanceof JsonArray) {
      List<String> queryList = new ArrayList<>();
      ((JsonArray) object)
          .forEach(
              e -> {
                if (isPrimitive(e)) {
                  queryList.add(getPrimitiveString(e));
                }
              });
      return String.format("(%s)", String.join(", ", queryList));
    } else {
      return "";
    }
  }

  private static boolean isPrimitive(Object object) {
    return object instanceof Boolean
        || object instanceof Number
        || object instanceof String
        || object instanceof Character;
  }

  private static String getPrimitiveString(Object object) {
    if (object instanceof String) {
      return String.format("'%s'", object);
    } else {
      return String.valueOf(object);
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
