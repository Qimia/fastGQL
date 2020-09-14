package dev.fastgql.sql;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface QueryExecutor {
  Observable<Map<String, Object>> apply(
      String query, List<RowExecutor> rowExecutors, QueryResponseComposer queryResponseComposer);

  default Observable<Map<String, Object>> justQuery(String query) {
    return apply(query, List.of(), (rowExecutors, row) -> Maybe.just(Map.of()));
  }
}
