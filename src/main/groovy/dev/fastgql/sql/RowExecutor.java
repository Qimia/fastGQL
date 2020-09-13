package dev.fastgql.sql;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import java.util.Map;

@FunctionalInterface
public interface RowExecutor {
  Single<Map<String, Object>> apply(QueryExecutor queryExecutor, Row row);
}
