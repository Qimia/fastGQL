package dev.fastgql.transaction;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.integration.DBTestUtils;
import dev.fastgql.integration.GraphQLTestUtils;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.VertxModule;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

public interface TransactionTest extends SetupTearDownForEachWithDelay {
  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  default void test(Vertx vertx, VertxTestContext context) {
    String directory = "transactions";
    System.out.println(String.format("Test: %s", directory));
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

    Injector injector = Guice.createInjector(new VertxModule(vertx, config), new DatabaseModule());

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
                GraphQLTestUtils.verifyQuerySimple(directory, getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
