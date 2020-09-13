package dev.fastgql.sql;

import io.reactivex.Maybe;
import io.vertx.reactivex.sqlclient.Row;
import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface QueryResponseComposer {
  Maybe<Map<String, Object>> apply(List<RowExecutor> rowExecutors, Row row);
}
