package dev.fastgql.sql;

import io.reactivex.Maybe;
import java.util.Set;
import java.util.function.Function;

public class ExecutionDefinition<T> {
  private final Function<QueryExecutor, Maybe<T>> queryExecutorResponseFunction;
  private final Set<String> queriedTables;

  ExecutionDefinition(
      Function<QueryExecutor, Maybe<T>> queryExecutorResponseFunction,
      Set<String> queriedTables) {
    this.queryExecutorResponseFunction = queryExecutorResponseFunction;
    this.queriedTables = queriedTables;
  }

  public Function<QueryExecutor, Maybe<T>> getQueryExecutorResponseFunction() {
    return queryExecutorResponseFunction;
  }

  public Set<String> getQueriedTables() {
    return queriedTables;
  }
}
