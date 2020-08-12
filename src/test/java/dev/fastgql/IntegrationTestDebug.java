package dev.fastgql;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.integration.*;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.VertxModule;
import io.reactivex.Completable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.sqlclient.Pool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@ExtendWith(VertxExtension.class)
public class IntegrationTestDebug {
  private final JdbcDatabaseContainer<?> jdbcDatabaseContainer = new MySQLContainer<>("fastgql/mysql-testcontainers:latest")
    .withNetworkAliases("mysql")
    .withUsername("debezium")
    .withPassword("dbz");

  private String deploymentID;
  private JsonObject config;
  private JsonObject configMultipleQueries;

  @BeforeEach
  public void beforeEach(Vertx vertx, VertxTestContext context) {
    Startables.deepStart(Stream.of(jdbcDatabaseContainer)).join();

    DatasourceConfig datasourceConfig =
      DatasourceConfig.createDatasourceConfig(
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword(),
        jdbcDatabaseContainer.getDatabaseName());

    config =
      new JsonObject()
        .put("debezium", Map.of("embedded", true, "server", "dbserver"))
        .put("http.port", 8081)
        .put(
          "datasource",
          Map.of(
            "jdbcUrl", datasourceConfig.getJdbcUrl(),
            "username", datasourceConfig.getUsername(),
            "password", datasourceConfig.getPassword(),
            "schema", datasourceConfig.getSchema()));

    configMultipleQueries =
      new JsonObject()
        .put("debezium", Map.of("embedded", true, "server", "dbserver"))
        .put("http.port", 8081)
        .put(
          "datasource",
          Map.of(
            "jdbcUrl", String.format("%s?allowMultiQueries=true", jdbcDatabaseContainer.getJdbcUrl()),
            "username", datasourceConfig.getUsername(),
            "password", datasourceConfig.getPassword(),
            "schema", datasourceConfig.getSchema()));

    DeploymentOptions options = new DeploymentOptions().setConfig(config);
    vertx
      .rxDeployVerticle(new FastGQL(), options)
      .subscribe(
        deploymentID -> {
          this.deploymentID = deploymentID;
          context.completeNow();
        });
  }

  @AfterEach
  public void afterEach(Vertx vertx, VertxTestContext context) {
    vertx
      .rxUndeploy(deploymentID)
      .subscribe(
        () -> {
          jdbcDatabaseContainer.close();
          context.completeNow();
        });
  }

  @Test
  public void shouldReceiveResponse(Vertx vertx, VertxTestContext context) {
    String directory = "subscriptions/simple/referencing";

    Injector injector =
      Guice.createInjector(new VertxModule(vertx, config), new DatabaseModule());
    Pool pool = injector.getInstance(Pool.class);

    Injector injectorMultipleQueries =
      Guice.createInjector(new VertxModule(vertx, configMultipleQueries), new DatabaseModule());
    Pool poolMultipleQueries = injectorMultipleQueries.getInstance(Pool.class);

    System.out.println(String.format("Test: %s", directory));

    WebClient client = WebClient.create(vertx);
    HttpClient httpClient =
      vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8081));
    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
      List.of(
        String.format("%s/expected-1.json", directory),
        String.format("%s/expected-2.json", directory));

    DBTestUtils.executeSQLQuery(Paths.get(directory, "init.sql").toString(), poolMultipleQueries)
      .flatMap(
        rows ->
          client
            .get(8081, "localhost", "/update")
            .rxSend()
            .flatMap(response -> httpClient.rxWebSocket("/graphql"))
            .flatMapCompletable(
              webSocket -> //Completable.complete())
                GraphQLTestUtils.startSubscription(query, context, webSocket))
            .delay(10, TimeUnit.SECONDS)
            .andThen(
              DBTestUtils.executeSQLQuery(
                String.format("%s/query.sql", directory), pool)))
      .subscribe(
        rows -> {
          System.out.println("affected rows: " + rows.rowCount());
          context.completeNow();
        }, context::failNow);
  }
}
