package dev.fastgql.transaction;

import dev.fastgql.integration.DBTestUtils;
import dev.fastgql.integration.GraphQLTestUtils;
import dev.fastgql.integration.ResourcesTestUtils;
import io.reactivex.Observable;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public interface TransactionTest extends SetupTearDownForEachWithDelay {

  @Test
  @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
  default void test(Vertx vertx, VertxTestContext context) throws IOException {
    String directory = "transactions";
    System.out.println(String.format("Test: %s", directory));

    WebClient client = WebClient.create(vertx);
    Pool pool = getPool(vertx);
    Pool poolMultipleQueries = getPoolMultipleQueries(vertx);

    String permissionsScript =
        ResourcesTestUtils.readResource(Paths.get(directory, "permissions.groovy").toString());

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
        .flatMap(
            rows ->
                client
                    .post(getDeploymentPort(), "localhost", "/v1/permissions")
                    .rxSendBuffer(Buffer.buffer(permissionsScript)))
        .flatMap(
            rows ->
                client
                    .get(getDeploymentPort(), "localhost", "/v1/update")
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
                GraphQLTestUtils.verifyQuerySimple(
                    directory, getDeploymentPort(), null, vertx, context),
            context::failNow);
  }
}
