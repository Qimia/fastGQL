package dev.fastgql.sql;

import io.reactivex.Single;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface SQLExecutor {
  Single<List<Map<String, Object>>> execute(String sqlQuery);
}
