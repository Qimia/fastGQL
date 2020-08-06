/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration;

import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.testcontainers.containers.JdbcDatabaseContainer;

/**
 * Test utils for executing SQL queries.
 *
 * @author Kamil Bobrowski
 */
public class DBTestUtils {

  /**
   * Execute SQL queries stored in a resource.
   *
   * @param sqlResource name of resource
   * @param jdbcUrl JDBC url
   * @param username database username
   * @param password database password
   * @throws SQLException when SQL query cannot be executed
   * @throws IOException when resource cannot be read
   */
  public static void executeSQLQueryFromResource(
      String sqlResource, String jdbcUrl, String username, String password)
      throws SQLException, IOException {
    try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        Statement statement = connection.createStatement()) {
      statement.execute(ResourcesTestUtils.readResource(sqlResource));
    }
  }

  /**
   * Execute SQL queries stored in a resource.
   *
   * @param sqlResource name of resource
   * @param jdbcDatabaseContainer database testcontainer
   * @throws SQLException when SQL query cannot be executed
   * @throws IOException when resource cannot be read
   */
  public static void executeSQLQueryFromResource(
      String sqlResource, JdbcDatabaseContainer<?> jdbcDatabaseContainer)
      throws IOException, SQLException {
    executeSQLQueryFromResource(
        sqlResource,
        jdbcDatabaseContainer.getJdbcUrl(),
        jdbcDatabaseContainer.getUsername(),
        jdbcDatabaseContainer.getPassword());
  }

  /**
   * Execute SQL queries stored in a resource with specified delay.
   *
   * @param sqlResource name of resource
   * @param jdbcDatabaseContainer database testcontainer
   * @param context vertx test context
   */
  public static void executeSQLQueryFromResourceWithContext(
      String sqlResource,
      JdbcDatabaseContainer<?> jdbcDatabaseContainer,
      VertxTestContext context) {
    try {
      DBTestUtils.executeSQLQueryFromResource(sqlResource, jdbcDatabaseContainer);
    } catch (SQLException | IOException e) {
      context.failNow(e);
    }
  }
}
