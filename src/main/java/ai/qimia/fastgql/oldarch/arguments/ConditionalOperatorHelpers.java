/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package ai.qimia.fastgql.oldarch.arguments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConditionalOperatorHelpers {

  public static String getConditionQuery(JsonObject obj) {
    if (obj.size() == 0) {
      return "TRUE ";
    }
    List<String> conditionList = obj.keySet().stream().map(m -> {
      String condition;
      switch (m) {
        case "_and":
          condition = getArrayQuery("AND ", obj.get(m).getAsJsonArray());
          break;
        case "_or":
          condition = getArrayQuery("OR ", obj.get(m).getAsJsonArray());
          break;
        case "_not":
          condition = getNotQuery(obj.get(m).getAsJsonObject());
          break;
        default:
          condition = getTypeComparisonQuery(m, obj.get(m).getAsJsonObject());
      }
      return String.format("(%s) ", condition);
    }).collect(Collectors.toList());
    return String.join("AND ", conditionList);
  }

  private static String getArrayQuery(String delimiter, JsonArray array) {
    List<String> list = new ArrayList<>();
    for (int i = 0; i < array.size(); i++) {
      list.add(
          String.format(
              "(%s) ",
              getConditionQuery(array.get(i).getAsJsonObject())));
    }
    return String.join(delimiter, list);
  }

  private static String getNotQuery(JsonObject object) {
    return String.format("NOT (%s) ", getConditionQuery(object));
  }

  private static String getTypeComparisonQuery(String key, JsonObject obj) {
    if (obj.size() == 0) {
      return "TRUE ";
    }
    List<String> list = obj.keySet().stream().map(opName -> {
      String operator = ConditionalOperatorTypes.getOperatorNameToValueMap().get(opName);
      String compareValue = getCompareValue(obj.get(opName));
      return String.format("(%s %s %s) ", key, operator, compareValue);
    }).collect(Collectors.toList());
    return String.join("AND ", list);
  }

  private static String getCompareValue(JsonElement element) {
    if (element.isJsonPrimitive()) {
      return getPrimitiveString(element.getAsJsonPrimitive());
    } else if (element.isJsonArray()) {
      List<String> list = new ArrayList<>();
      element.getAsJsonArray().forEach(e -> {
        if (e.isJsonPrimitive()) {
          list.add(getPrimitiveString(e.getAsJsonPrimitive()));
        }
      });
      return String.format("(%s)", String.join(", ", list));
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
