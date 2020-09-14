package dev.fastgql.sql;

import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;

@FunctionalInterface
public interface QueryExecutor {
  Single<RowSet<Row>> apply(String query);
}
