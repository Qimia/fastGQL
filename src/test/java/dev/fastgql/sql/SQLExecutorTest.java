/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class SQLExecutorTest {

  @Test
  void testDefaultConstructor() {
    SQLExecutor sqlExecutor = new SQLExecutor();
    sqlExecutor
        .getSqlExecutorFunction()
        .apply("testQuery")
        .test()
        .assertNoErrors()
        .assertValue(List::isEmpty);
  }

  @Test
  void testConstructorWithArgument() {
    Function<String, Single<List<Map<String, Object>>>> function =
        query -> Single.just(List.of(Map.of(query, query)));
    SQLExecutor sqlExecutor = new SQLExecutor(function);
    sqlExecutor
        .getSqlExecutorFunction()
        .apply("testQuery")
        .test()
        .assertNoErrors()
        .assertValue(l -> l.size() == 1)
        .assertValue(l -> l.get(0).get("testQuery") == "testQuery");
  }

  @Test
  void testSetSqlExecutorFunction() {
    SQLExecutor sqlExecutor = new SQLExecutor();
    sqlExecutor.setSqlExecutorFunction(query -> Single.just(List.of(Map.of(query, query))));
    sqlExecutor
        .getSqlExecutorFunction()
        .apply("testQuery")
        .test()
        .assertNoErrors()
        .assertValue(l -> l.size() == 1)
        .assertValue(l -> l.get(0).get("testQuery") == "testQuery");
  }
}
