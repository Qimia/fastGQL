/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SQLArgumentsTest {

  static Stream<Arguments> getTestSQLArgumentsParameters() {
    return Stream.of(
        Arguments.of(Map.of("limit", 1), Map.of("limit", 1)),
        Arguments.of(Map.of("offset", 1), Map.of("offset", 1)),
        Arguments.of(
            Map.of("order_by", List.of(Map.of("id1", "ASC"), Map.of("id2", "DESC"))),
            Map.of("order_by", new JsonArray("[{\"id1\": \"ASC\"}, {\"id2\": \"DESC\"}]"))),
        Arguments.of(
            Map.of("where", Map.of("id", Map.of("_eq", 1))),
            Map.of("where", new JsonObject("{\"id\": {\"_eq\": 1}}"))));
  }

  @ParameterizedTest(name = "testSQLArguments {index} => Test: [{arguments}]")
  @MethodSource("getTestSQLArgumentsParameters")
  void getters(Map<String, Object> input, Map<String, Object> expected) {
    SQLArguments sqlArguments = new SQLArguments(input);
    assertEquals(expected.get("limit"), sqlArguments.getLimit());
    assertEquals(expected.get("offset"), sqlArguments.getOffset());
    assertEquals(expected.get("order_by"), sqlArguments.getOrderBy());
    assertEquals(expected.get("where"), sqlArguments.getWhere());
  }
}
