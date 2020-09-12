package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.newsql.ExecutionFunctions;
import dev.fastgql.newsql.QueryExecutor;
import dev.fastgql.sql.SQLExecutor;
import dev.fastgql.sql.SQLUtils;
import io.reactivex.Observable;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.Transaction;

import java.util.Optional;
import java.util.function.Function;

public class SQLExecutorModule extends AbstractModule {

  @Provides
  Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
    return transaction -> query -> transaction.rxQuery(query).map(SQLUtils::rowSetToList);
  }

  @Provides
  Function<Transaction, QueryExecutor> provideTransactionQueryExecutorFunction() {
    return transaction -> (query, rowExecutors, queryResponseComposer) -> transaction.rxQuery(query)
      .doOnSuccess(rows -> System.out.println("EXECUTED: " + query))
      .flatMapObservable(Observable::fromIterable)
      .map(Optional::of)
      .defaultIfEmpty(Optional.empty())
      .flatMapSingle(row -> row.isPresent() ? queryResponseComposer.apply(rowExecutors, row.get()) : queryResponseComposer.apply(rowExecutors, null));
  }
}
