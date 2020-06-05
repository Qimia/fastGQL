package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SQLExecutor {
  private Function<String, Single<List<Map<String, Object>>>> sqlExecutorFunction;

  public SQLExecutor() {
    this.sqlExecutorFunction = query -> Single.just(List.of());
  }

  public SQLExecutor(Function<String, Single<List<Map<String, Object>>>> sqlExecutor) {
    this.sqlExecutorFunction = sqlExecutor;
  }

  public void setSqlExecutorFunction(
      Function<String, Single<List<Map<String, Object>>>> sqlExecutor) {
    this.sqlExecutorFunction = sqlExecutor;
  }

  public Function<String, Single<List<Map<String, Object>>>> getSqlExecutorFunction() {
    return sqlExecutorFunction;
  }
}
