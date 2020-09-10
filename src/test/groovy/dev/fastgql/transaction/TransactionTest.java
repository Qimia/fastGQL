package dev.fastgql.transaction;

import dev.fastgql.integration.DBTestUtils;
import dev.fastgql.integration.GraphQLTestUtils;
import io.reactivex.Observable;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public interface TransactionTest extends SetupTearDownForEachWithDelay {
  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  default void test(Vertx vertx, VertxTestContext context) {
    String directory = "transactions";
    System.out.println(String.format("Test: %s", directory));

    WebClient client = WebClient.create(vertx);
    Pool pool = getPool(vertx);
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
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
                                .subscribe()))
        .subscribe(
            response ->
                GraphQLTestUtils.verifyQuerySimple(directory, getDeploymentPort(), vertx, context),
            context::failNow);
  }
}
