/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SQLUtilsTest {
  @Test
  void testIsReferencingName() {
    assertTrue(SQLUtils.isReferencingName("test_ref"));
    assertFalse(SQLUtils.isReferencingName("test"));
  }

  @Test
  void shouldCreateOrderByQuery() {
    String expected = "a0.a ASC, b0.c DESC";
    String jsonString = "[{\"a\": \"ASC\"}, {\"b_ref\": {\"c\": \"DESC\"}}]";
    JsonArray array = new Gson().fromJson(jsonString, JsonArray.class);
    Map<String, String> tableFieldToAlias = Map.of("b_ref", "b0");
    String actual = SQLUtils.createOrderByQuery(array, "a0", tableFieldToAlias);
    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = "testCreateBoolQuery {index} => Test: [json: {arguments}]")
  @MethodSource("getTestCreateBoolQueryParameters")
  void testCreateBoolQuery(String jsonString, String aliasName, String expectedQuery) {
    JsonObject object = new Gson().fromJson(jsonString, JsonObject.class);
    Map<String, String> tableFieldToAlias = Map.of();
    String actualQuery = SQLUtils.createBoolQuery(object, aliasName, tableFieldToAlias);
    assertEquals(expectedQuery, actualQuery);
  }

  static Stream<Arguments> getTestCreateBoolQueryParameters() {
    return Stream.of(
        Arguments.of("{}", "", "TRUE"),
        Arguments.of("{\"id\": {\"_eq\": 1}}", "a0", "((a0.id = 1))"),
        Arguments.of("{\"id\": {\"_gt\": 1, \"_lt\": 4}}", "a0", "((a0.id > 1) AND (a0.id < 4))"),
        Arguments.of("{\"_and\": [{\"id\": {\"_eq\": 1}}]}", "a0", "((((a0.id = 1))))"),
        Arguments.of(
            "{\"_or\": [{\"id\": {\"_eq\": 1}}, {\"id\": {\"_eq\": 2}}]}",
            "a0",
            "((((a0.id = 1))) OR (((a0.id = 2))))"),
        Arguments.of("{\"_not\": {}}", "a0", "(NOT (TRUE))"));
  }

  @Test
  void shouldCreateBoolQueryTrueReturnTRUE() {
    String expected = "TRUE";
    JsonObject object = new JsonObject();
    Map<String, String> emptyTableFieldToAlias = new HashMap<>();
    String actual = SQLUtils.createBoolQuery(object, "", emptyTableFieldToAlias);
    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = "testRemoveRedundantParentheses {index} => Test: [{arguments}]")
  @MethodSource("getTestRemoveRedundantParenthesesParameters")
  void testRemoveRedundantParentheses(String query, String expected) {
    assertEquals(expected, SQLUtils.removeRedundantParentheses(query));
  }

  static Stream<Arguments> getTestRemoveRedundantParenthesesParameters() {
    return Stream.of(
        Arguments.of("(((a))b)", "((a)b)"),
        Arguments.of(" ( () a (b))", "( a (b))"),
        Arguments.of("((a) b (c))  ( )", "((a) b (c))"));
  }
}
