package dev.fastgql.newsql;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;

import java.util.List;
import java.util.Map;

@FunctionalInterface
public interface QueryResponseComposer {
  Single<Map<String, Object>> apply(List<RowExecutor> rowExecutors, Row row);
}
