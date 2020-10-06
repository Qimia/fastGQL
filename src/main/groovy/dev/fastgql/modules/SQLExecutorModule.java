package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.QueryExecutor;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.*;
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
                  ? transaction.preparedQuery(query).rxExecute(Tuple.wrap(params))
                  : transaction.query(query).rxExecute();

          return result
              .doOnSuccess(rows -> log.info("[executed] {} {}", query, params))
              .doOnError(error -> log.error("[error executing] {} {}", query, params));
        };
  }
}
