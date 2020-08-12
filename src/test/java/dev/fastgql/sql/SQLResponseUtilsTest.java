/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.reactivex.Single;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class SQLResponseUtilsTest {

  @ParameterizedTest(name = "testGetValue {index} => Test: [{arguments}]")
  @MethodSource("getTestGetValueParameters")
  void getValue(Map<String, Object> row, String tableAlias, String tableKeyName, Object expected) {
    assertEquals(expected, SQLResponseUtils.getValue(row, tableAlias, tableKeyName));
  }

  static Stream<Arguments> getTestGetValueParameters() {
    return Stream.of(
        Arguments.of(Map.of("t1_k1", "v1"), "t1", "k1", "v1"),
        Arguments.of(Map.of("t2_k2", 1), "t2", "k2", 1),
        Arguments.of(Map.of("t3", 1), "t3", "k3", null));
  }

  @Test
  void constructResponse() {
    Map<String, Integer> expected = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      expected.put(String.format("k%d", i), i);
    }

    List<Component> components = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      ComponentRow componentRow = new ComponentRow(String.format("k%d", i));
      componentRow.setParentTableAlias(String.format("t%d", i));
      components.add(componentRow);
    }
    Map<String, Object> row = new HashMap<>();
    for (int i = 0; i < 10; i++) {
      row.put(String.format("t%d_k%d", i, i), i);
    }
    Single<Map<String, Object>> response =
        SQLResponseUtils.constructResponse(null, row, components);
    response.test().assertNoErrors().assertValue(l -> l.equals(expected));
  }
}
