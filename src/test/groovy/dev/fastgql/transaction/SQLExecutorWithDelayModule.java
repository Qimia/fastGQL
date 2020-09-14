package dev.fastgql.transaction;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.QueryExecutor;
import io.reactivex.Observable;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLExecutorWithDelayModule extends AbstractModule {

  private static final Logger log = LoggerFactory.getLogger(SQLExecutorWithDelayModule.class);

  private final long delay;
  private final TimeUnit timeUnit;

  public SQLExecutorWithDelayModule(long delay, TimeUnit timeUnit) {
    this.delay = delay;
    this.timeUnit = timeUnit;
  }

  @Provides
  Function<Transaction, QueryExecutor> provideTransactionQueryExecutorFunction() {
    return transaction ->
        (query, rowExecutors, queryResponseComposer) ->
            transaction
                .rxQuery(query)
                .doOnSuccess(rows -> log.info("[executing] {}", query))
                .delay(query.startsWith("SELECT") ? delay : 0, timeUnit)
                .doOnSuccess(result -> log.info("[response] {}", query))
                .flatMapObservable(Observable::fromIterable)
                .flatMapMaybe(row -> queryResponseComposer.apply(rowExecutors, row));
  }

  // @Provides
  // Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
  //  return transaction ->
  //      query ->
  //          transaction
  //              .rxQuery(query)
  //              .doOnSuccess(rows -> log.info("[executing] {}", query))
  //              .map(SQLUtils::rowSetToList)
  //              .delay(query.startsWith("SELECT") ? delay : 0, timeUnit)
  //              .doOnSuccess(result -> log.info("[response] {}", query));
  // }
}
