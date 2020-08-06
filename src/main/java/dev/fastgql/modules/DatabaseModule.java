package dev.fastgql.modules;

import dagger.Module;
import dagger.Provides;
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
import java.util.function.Supplier;
import javax.inject.Singleton;

@Module
public abstract class DatabaseModule {

  @Provides
  @Singleton
  static Supplier<DatabaseSchema> provideDatabaseSchemaSupplier(DatasourceConfig datasourceConfig) {
    return () -> {
      try (Connection connection = datasourceConfig.getConnection()) {
        return MetadataUtils.createDatabaseSchema(connection);
      } catch (SQLException e) {
        e.printStackTrace();
        return null;
      }
    };
  }

  @Provides
  @Singleton
  static Pool providePool(Vertx vertx, DatasourceConfig datasourceConfig) {
    return datasourceConfig.getPool(vertx);
  }

  @Provides
  @Singleton
  static Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
    return transaction -> query -> transaction.rxQuery(query).map(SQLUtils::rowSetToList);
  }
}
