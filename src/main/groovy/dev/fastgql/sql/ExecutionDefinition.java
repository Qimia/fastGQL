package dev.fastgql.sql;

import io.reactivex.Maybe;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ExecutionDefinition {
  private final Function<QueryExecutor, Maybe<List<Map<String, Object>>>>
      queryExecutorResponseFunction;
  private final Set<String> queriedTables;

  ExecutionDefinition(
      Function<QueryExecutor, Maybe<List<Map<String, Object>>>> queryExecutorResponseFunction,
      Set<String> queriedTables) {
    this.queryExecutorResponseFunction = queryExecutorResponseFunction;
    this.queriedTables = queriedTables;
  }

  public Function<QueryExecutor, Maybe<List<Map<String, Object>>>>
      getQueryExecutorResponseFunction() {
    return queryExecutorResponseFunction;
  }

  public Set<String> getQueriedTables() {
    return queriedTables;
  }
}
