/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql.integration;

import io.reactivex.Observable;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class DBTestUtils {
  public static void executeSQLQueryFromResource(
      String sqlResource, String jdbcUrl, String username, String password)
      throws SQLException, IOException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute(TestUtils.readResource(sqlResource));
    }
  }

  public static void executeSQLQueryFromResource(
      String sqlResource, JdbcDatabaseContainer<?> jdbcDatabaseContainer)
      throws IOException, SQLException {
    executeSQLQueryFromResource(
        sqlResource,
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword());
  }

  public static void executeSQLQueryFromResourceWithDelay(
      String sqlResource,
      long delay,
      TimeUnit unit,
      JdbcDatabaseContainer<?> jdbcDatabaseContainer,
      VertxTestContext context) {
    Observable.timer(delay, unit)
        .doOnComplete(
            () -> {
              try {
                DBTestUtils.executeSQLQueryFromResource(sqlResource, jdbcDatabaseContainer);
              } catch (SQLException e) {
                context.failNow(e);
              }
            })
        .subscribe();
  }
}
