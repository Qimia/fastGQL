package dev.fastgql;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import dev.fastgql.integration.ContainerEnvWithDatabase;
import dev.fastgql.integration.DBTestUtils;
import dev.fastgql.integration.GraphQLTestUtils;
import dev.fastgql.integration.SetupTearDownForEach;
import dev.fastgql.integration.WithPostgres;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.GraphQLModule;
import dev.fastgql.modules.ServerModule;
import dev.fastgql.modules.VertxModule;
import dev.fastgql.sql.SQLExecutor;
import dev.fastgql.sql.SQLUtils;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Transaction;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;

@ExtendWith(VertxExtension.class)
public class TransactionTest {

  private static final Logger log = LoggerFactory.getLogger(TransactionTest.class);

  static class SQLExecutorWithDelayModule extends AbstractModule {

    private final long delay;
    private final TimeUnit timeUnit;

    public SQLExecutorWithDelayModule(long delay, TimeUnit timeUnit) {
      this.delay = delay;
      this.timeUnit = timeUnit;
    }

    @Provides
    protected Function<Transaction, SQLExecutor> provideTransactionSQLExecutorFunction() {
      return transaction ->
          query ->
              transaction
                  .rxQuery(query)
                  .doOnSuccess(rows -> log.info("[executing] {}", query))
                  .map(SQLUtils::rowSetToList)
                  .delay(query.startsWith("SELECT") ? delay : 0, timeUnit)
                  .doOnSuccess(result -> log.info("[response] {}", query))
                  .doOnError(error -> log.error(error.toString()));
    }
  }

  static class FastGQLWithDelay extends FastGQL {
    @Override
    protected Injector createInjector() {
      return Guice.createInjector(
          new VertxModule(vertx, config()),
          new ServerModule(),
          new GraphQLModule(),
          new DatabaseModule(),
          new SQLExecutorWithDelayModule(10, TimeUnit.SECONDS));
    }
  }

  interface SetupTearDownForEachCustom extends SetupTearDownForEach {
    @BeforeEach
    default void beforeEach(Vertx vertx, VertxTestContext context) {
      setup(vertx, context, new FastGQLWithDelay());
    }
  }

  @Nested
  class PostgresTransactionTest extends ContainerEnvWithDatabase
      implements WithPostgres, SetupTearDownForEachCustom {

    @Test
    @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
    public void test(Vertx vertx, VertxTestContext context) {
      String directory = "transactions";
      log.info("Test: {}", directory);
      try {
        DBTestUtils.executeSQLQueryFromResource(
            Paths.get(directory, "init.sql").toString(),
            getJdbcUrlForMultipleQueries(),
            getJdbcDatabaseContainer().getUsername(),
            getJdbcDatabaseContainer().getPassword());
      } catch (SQLException | IOException e) {
        context.failNow(e);
        return;
      }

      JdbcDatabaseContainer<?> jdbcDatabaseContainer = getJdbcDatabaseContainer();

      JsonObject config =
          new JsonObject(
              Map.of(
                  "datasource",
                  Map.of(
                      "jdbcUrl", jdbcDatabaseContainer.getJdbcUrl(),
                      "username", jdbcDatabaseContainer.getUsername(),
                      "password", jdbcDatabaseContainer.getPassword(),
                      "schema", "public")));

      Injector injector =
          Guice.createInjector(new VertxModule(vertx, config), new DatabaseModule());

      Pool pool = injector.getInstance(Pool.class);

      WebClient client = WebClient.create(vertx);
      client
          .get(getDeploymentPort(), "localhost", "/update")
          .rxSend()
          .doOnSuccess(
              response ->
                  Observable.timer(5, TimeUnit.SECONDS)
                      .flatMap(
                          timeout ->
                              DBTestUtils.executeSQLQuery(
                                      Paths.get(directory, "modify.sql").toString(), pool)
                                  .toObservable())
                      .subscribe())
          .subscribe(
              response ->
                  GraphQLTestUtils.verifyQuerySimple(
                      directory, getDeploymentPort(), vertx, context),
              context::failNow);
    }
  }
}
