package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.MetadataUtils;
import dev.fastgql.sql.SQLExecutor;
import dev.fastgql.sql.SQLUtils;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Transaction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

public class DatabaseModule extends AbstractModule {
  @Provides
  Function<Connection, DatabaseSchema> provideConnectionDatabaseSchemaFunction() {
    return connection -> {
      DatabaseSchema databaseSchema;
      try {
        databaseSchema = MetadataUtils.createDatabaseSchema(connection);
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
      return databaseSchema;
    };
  }

  @Provides
  Pool providePool(DatasourceConfig datasourceConfig, Vertx vertx) {
    return datasourceConfig.getPool(vertx);
  }

  @Provides
  Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
    return transaction -> query -> transaction.rxQuery(query).map(SQLUtils::rowSetToList);
  }
}
