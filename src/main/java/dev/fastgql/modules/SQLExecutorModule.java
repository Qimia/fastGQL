package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.SQLExecutor;
import dev.fastgql.sql.SQLUtils;
import io.vertx.reactivex.sqlclient.SqlConnection;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLExecutorModule extends AbstractModule {

  private static final Logger log = LoggerFactory.getLogger(SQLExecutorModule.class);

  @Provides
  protected Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
    return transaction ->
        query -> {
          log.info("Defining query: " + query);
          return transaction
              .rxQuery(query)
              .doOnSuccess(rows -> log.info("Query executed: " + query))
              .doOnError(error -> log.error("Query failed: " + query + " " + error.getMessage()))
              .map(SQLUtils::rowSetToList);
        };
  }

  @Provides
  protected Function<SqlConnection, SQLExecutor> provideConnectionSQLExecutorFunction() {
    return connection ->
      query -> {
        log.info("Defining query: " + query);
        return connection
          .rxQuery(query)
          .doOnSuccess(rows -> log.info("Query executed: " + query))
          .doOnError(error -> log.error("Query failed: " + query + " " + error.getMessage()))
          .map(SQLUtils::rowSetToList);
      };
  }
}
