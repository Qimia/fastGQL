package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.QueryExecutor;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import io.vertx.reactivex.sqlclient.Transaction;
import io.vertx.reactivex.sqlclient.Tuple;

import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLExecutorModule extends AbstractModule {

  private static final Logger log = LoggerFactory.getLogger(SQLExecutorModule.class);

  @Provides
  Function<Transaction, QueryExecutor> provideTransactionQueryExecutorFunction() {
    return transaction ->
        (query, params) -> {
          Single<RowSet<Row>> result =
              params != null && params.size() > 0
                  ? transaction.rxPreparedQuery(query, Tuple.wrap(params))
                  : transaction.rxQuery(query);

          return result
              .doOnSuccess(rows -> log.info("[executed] {} {}", query, params))
              .doOnError(error -> log.error("[error executing] {} {}", query, params));
        };
  }
}
