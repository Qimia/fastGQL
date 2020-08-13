package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.SQLExecutor;
import dev.fastgql.sql.SQLExecutorRowSet;
import dev.fastgql.sql.SQLUtils;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.function.Function;

public class SQLExecutorModule extends AbstractModule {

  @Provides
  Function<Transaction, SQLExecutorRowSet> provideTransactionSQLExecutorRowSetFunction() {
    return transaction -> transaction::rxQuery;
  }

  @Provides
  Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction(
      Function<Transaction, SQLExecutorRowSet> transactionSQLExecutorRowSetFunction) {
    return transaction ->
        query ->
            transactionSQLExecutorRowSetFunction
                .apply(transaction)
                .execute(query)
                .map(SQLUtils::rowSetToList);
  }
}
