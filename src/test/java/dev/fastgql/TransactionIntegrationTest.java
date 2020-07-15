package dev.fastgql;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.integration.DBTestUtils;
import dev.fastgql.sql.AliasGenerator;
import dev.fastgql.sql.Component;
import dev.fastgql.sql.ComponentReferenced;
import dev.fastgql.sql.ComponentRow;
import dev.fastgql.sql.ExecutionRoot;
import dev.fastgql.sql.SQLArguments;
import io.reactivex.Single;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.SqlConnection;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

@ExtendWith(VertxExtension.class)
public class TransactionIntegrationTest {

  private static final AliasGenerator aliasGenerator = new AliasGenerator();
  private static final SQLArguments emptyArgs = new SQLArguments(Map.of());

  private static final JdbcDatabaseContainer<?> databaseContainer = new MySQLContainer<>("mysql:8");

  private static final List<Map<String, Object>> expected =
      List.of(Map.of("id", 0, "customers_on_address", List.of(Map.of("id", 0), Map.of("id", 1))));

  private static ExecutionRoot executionRoot;
  private static Pool rootClient;
  private static Pool testClient;

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) {
    Startables.deepStart(Stream.of(databaseContainer)).join();
    String jdbcUrl = String.format("%s?allowMultiQueries=true", databaseContainer.getJdbcUrl());
    String databaseName = databaseContainer.getDatabaseName();

    rootClient =
        DatasourceConfig.createDatasourceConfig(jdbcUrl, "root", "test", databaseName)
            .getPool(vertx);
    testClient =
        DatasourceConfig.createDatasourceConfig(jdbcUrl, "test", "test", databaseName)
            .getPool(vertx);

    try {
      DBTestUtils.executeSQLQueryFromResource(
          Paths.get("transaction", "init.sql").toString(), jdbcUrl, "root", "test");
    } catch (IOException | SQLException e) {
      context.failNow(e);
    }

    executionRoot = getExecutorRoot();

    context.completeNow();
  }

  @AfterEach
  public void tearDown(Vertx vertx, VertxTestContext context) throws SQLException {
    databaseContainer.close();
    context.completeNow();
  }

  @Test
  public void shouldNotEquals(Vertx vertx, VertxTestContext context) {
    testClient
        .rxGetConnection()
        .doOnSuccess(
            connection -> {
              executionRoot.setSqlExecutor(
                  query -> executeQuery(query, connection).delay(5, TimeUnit.SECONDS));
              executionRoot
                  .execute()
                  .doOnSuccess(
                      v -> {
                        Assertions.assertNotEquals(expected, v);
                        System.out.println(v);
                        connection.close();
                        context.completeNow();
                      })
                  .subscribe();
            })
        .subscribe();

    rootClient
        .rxGetConnection()
        .doOnSuccess(
            connection ->
                connection
                    .rxQuery("INSERT INTO customers VALUES (1, 0)")
                    .delay(3, TimeUnit.SECONDS)
                    .doOnSuccess(
                        rows -> {
                          connection
                              .rxQuery("INSERT INTO customers VALUES (2, 0)")
                              .doOnSuccess(
                                  rows1 -> {
                                    connection.close();
                                  })
                              .subscribe();
                        })
                    .subscribe())
        .subscribe();

    System.out.println("complete");
  }

  private static Single<List<Map<String, Object>>> executeQuery(
      String query, SqlConnection connection) {
    return connection
        .rxQuery(query)
        .map(
            rowSet -> {
              List<String> columnNames = rowSet.columnsNames();
              List<Map<String, Object>> retList = new ArrayList<>();
              rowSet.forEach(
                  row -> {
                    Map<String, Object> r = new HashMap<>();
                    columnNames.forEach(columnName -> r.put(columnName, row.getValue(columnName)));
                    retList.add(r);
                  });
              return retList;
            });
  }

  private static ExecutionRoot getExecutorRoot() {
    Component customersOnAddress =
        new ComponentReferenced(
            "customers_on_address",
            "id",
            "customers",
            aliasGenerator.getAlias(),
            "address",
            emptyArgs);
    customersOnAddress.addComponent(new ComponentRow("id"));
    ExecutionRoot executionRoot =
        new ExecutionRoot("addresses", aliasGenerator.getAlias(), emptyArgs);
    executionRoot.addComponent(customersOnAddress);
    executionRoot.addComponent(new ComponentRow("id"));
    return executionRoot;
  }
}
