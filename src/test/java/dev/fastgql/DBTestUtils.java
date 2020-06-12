/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql;

import dev.fastgql.db.DatasourceConfig;
import io.reactivex.Observable;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class DBTestUtils {

  public static void executeSQLQuery(
      String sqlQuery, String jdbcUrl, String username, String password) throws SQLException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute(sqlQuery);
    }
  }

  public static void executeSQLQueryFromResource(
      String sqlResource, String jdbcUrl, String username, String password)
      throws SQLException, IOException {
    executeSQLQuery(TestUtils.readResource(sqlResource), jdbcUrl, username, password);
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

  public static void executeSQLQuery(
      String sqlQuery, JdbcDatabaseContainer<?> jdbcDatabaseContainer) throws SQLException {
    executeSQLQuery(
        sqlQuery,
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword());
  }

  /*
    public static void executeSQLQuery(
        String sqlQuery, PostgreSQLContainer<?> postgresContainer, VertxTestContext context) {
      try {
        executeSQLQuery(sqlQuery, postgresContainer);
      } catch (SQLException e) {
        context.failNow(e);
      }
    }
  */

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void executeSQLQueryWithDelay(
      String sqlQuery,
      long delay,
      TimeUnit unit,
      JdbcDatabaseContainer<?> jdbcDatabaseContainer,
      VertxTestContext context) {
    Observable.timer(delay, unit)
        .subscribe(
            result -> {
              try {
                DBTestUtils.executeSQLQuery(sqlQuery, jdbcDatabaseContainer);
              } catch (SQLException e) {
                context.failNow(e);
              }
            });
  }

  public static void executeSQLQueryFromResourceWithDelay(
      String sqlResource,
      long delay,
      TimeUnit unit,
      JdbcDatabaseContainer<?> jdbcDatabaseContainer,
      VertxTestContext context) {
    Observable.timer(delay, unit)
        .subscribe(
            result -> {
              System.out.println("Execute sql query");
              try {
                DBTestUtils.executeSQLQueryFromResource(sqlResource, jdbcDatabaseContainer);
              } catch (SQLException e) {
                context.failNow(e);
              }
            });
  }

  public static void executeSQLQueryFromResource(
      String sqlResource, PostgreSQLContainer<?> postgresContainer)
      throws IOException, SQLException {
    executeSQLQueryFromResource(
        sqlResource,
        postgresContainer.getJdbcUrl(),
        postgresContainer.getUsername(),
        postgresContainer.getPassword());
  }

  public static void executeSQLQueryFromResource(
      String sqlResource, MySQLContainer<?> mySQLContainer) throws IOException, SQLException {
    executeSQLQueryFromResource(
        sqlResource,
        String.format("%s?allowMultiQueries=true", mySQLContainer.getJdbcUrl()),
        mySQLContainer.getUsername(),
        mySQLContainer.getPassword());
  }

  public static DatasourceConfig datasourceConfig(PostgreSQLContainer<?> postgresContainer) {
    return DatasourceConfig.createDatasourceConfig(
        postgresContainer.getJdbcUrl(),
        postgresContainer.getUsername(),
        postgresContainer.getPassword(),
        "public");
  }

  public static DatasourceConfig datasourceConfig(MySQLContainer<?> mySQLContainer) {
    return DatasourceConfig.createDatasourceConfig(
        mySQLContainer.getJdbcUrl(),
        mySQLContainer.getUsername(),
        mySQLContainer.getPassword(),
        mySQLContainer.getDatabaseName());
  }
}
