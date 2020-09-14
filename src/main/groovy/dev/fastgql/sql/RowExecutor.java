package dev.fastgql.sql;

import io.reactivex.Maybe;
import io.vertx.reactivex.sqlclient.Row;
import java.util.Map;

@FunctionalInterface
public interface RowExecutor {
  Maybe<Map.Entry<String, Object>> apply(QueryExecutor queryExecutor, Row row);
}
