package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.sql.SQLExecutor;
import dev.fastgql.sql.SQLUtils;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.function.Function;

public class SQLExecutorModule extends AbstractModule {

  @Provides
  Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
    return transaction -> query -> transaction.rxQuery(query).map(SQLUtils::rowSetToList);
  }
}
