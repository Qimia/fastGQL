package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.QueryExecutor;
import io.reactivex.Observable;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLExecutorModule extends AbstractModule {

  private static final Logger log = LoggerFactory.getLogger(SQLExecutorModule.class);

  @Provides
  Function<Transaction, QueryExecutor> provideTransactionQueryExecutorFunction() {
    return transaction ->
        (query, rowExecutors, queryResponseComposer) ->
            transaction
                .rxQuery(query)
                .doOnSuccess(rows -> log.info("[executed] {}", query))
                .flatMapObservable(Observable::fromIterable)
                .flatMapMaybe(row -> queryResponseComposer.apply(rowExecutors, row));
  }
}
