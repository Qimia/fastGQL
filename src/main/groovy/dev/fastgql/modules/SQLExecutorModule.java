package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.QueryExecutor;
import io.reactivex.Observable;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.function.Function;

public class SQLExecutorModule extends AbstractModule {

  @Provides
  Function<Transaction, QueryExecutor> provideTransactionQueryExecutorFunction() {
    return transaction ->
        (query, rowExecutors, queryResponseComposer) ->
            transaction
                .rxQuery(query)
                .doOnSuccess(rows -> System.out.println("EXECUTED: " + query))
                .flatMapObservable(Observable::fromIterable)
                .flatMapMaybe(row -> queryResponseComposer.apply(rowExecutors, row));
  }
}
