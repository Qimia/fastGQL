package dev.fastgql.sql;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;

import java.util.List;

@FunctionalInterface
public interface QueryExecutor {
  Single<RowSet<Row>> apply(String query, List<Object> params);

  default Single<RowSet<Row>> apply(String query) {
    return apply(query, List.of());
  }
}
