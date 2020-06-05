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
import dev.fastgql.graphql.arguments.ConditionalOperatorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SQLUtils {

  public static boolean isReferencingName(String name) {
    return name.endsWith("_ref");
  }

  public static String buildBoolQuery(
      JsonObject obj, String fieldName, Map<String, String> tableFieldToAlias) {
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
                            obj.get(key).getAsJsonArray(), " AND ", fieldName, tableFieldToAlias);
                  } else if (key.equals("_or")) {
                    query =
                        getArrayQuery(
                            obj.get(key).getAsJsonArray(), " OR ", fieldName, tableFieldToAlias);
                  } else if (key.equals("_not")) {
                    query =
                        getNotQuery(obj.get(key).getAsJsonObject(), fieldName, tableFieldToAlias);
                  } else if (isReferencingName(key)) {
                    if (tableFieldToAlias.containsKey(key)) {
                      query =
                          buildBoolQuery(
                              obj.get(key).getAsJsonObject(),
                              tableFieldToAlias.get(key),
                              tableFieldToAlias);
                    } else {
                      query = "";
                    }
                  } else {
                    query = getComparisonQuery(obj.get(key).getAsJsonObject(), fieldName, key);
                  }
                  return String.format("(%s)", query);
                })
            .collect(Collectors.toList());
    return String.join(" AND ", queryList);
  }

  private static String getArrayQuery(
      JsonArray array, String delimiter, String fieldName, Map<String, String> tableFieldToAlias) {
    List<String> queryList = new ArrayList<>();
    for (int i = 0; i < array.size(); i++) {
      queryList.add(
          String.format(
              "(%s)",
              buildBoolQuery(array.get(i).getAsJsonObject(), fieldName, tableFieldToAlias)));
    }
    return String.join(delimiter, queryList);
  }

  private static String getNotQuery(
      JsonObject object, String fieldName, Map<String, String> tableFieldToAlias) {
    return String.format("NOT (%s)", buildBoolQuery(object, fieldName, tableFieldToAlias));
  }

  private static String getComparisonQuery(JsonObject object, String fieldName, String key) {
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
                  return String.format("(%s.%s %s %s)", fieldName, key, operator, compareValue);
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
}
