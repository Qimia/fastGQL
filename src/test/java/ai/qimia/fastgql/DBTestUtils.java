/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package ai.qimia.fastgql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import ai.qimia.fastgql.oldarch.DatasourceConfig;
import io.reactivex.Observable;
import io.vertx.junit5.VertxTestContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

public class DBTestUtils {

  public static void executeSQLQuery(String sqlQuery, String jdbcUrl,
      String username, String password) throws SQLException {
    try (
        Connection connection = DriverManager.getConnection(
            jdbcUrl,
            username,
            password
        );
        Statement statement = connection.createStatement()
    ) {
      statement.execute(sqlQuery);
    }
  }

  public static void executeSQLQueryFromResource(String sqlResource, String jdbcUrl,
      String username, String password) throws SQLException, IOException {
    executeSQLQuery(TestUtils.readResource(sqlResource), jdbcUrl, username, password);
  }

  public static void executeSQLQuery(String sqlQuery, PostgreSQLContainer<?> postgresContainer)
      throws SQLException {
    executeSQLQuery(sqlQuery, postgresContainer.getJdbcUrl(), postgresContainer.getUsername(),
        postgresContainer.getPassword());
  }

  public static void executeSQLQuery(String sqlQuery, PostgreSQLContainer<?> postgresContainer, VertxTestContext context) {
    try {
      executeSQLQuery(sqlQuery, postgresContainer);
    } catch (SQLException e) {
      context.failNow(e);
    }
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void executeSQLQueryWithDelay(String sqlQuery, long delay, TimeUnit unit, PostgreSQLContainer<?> postgresContainer, VertxTestContext context) {
    Observable.timer(delay, unit)
      .subscribe(
        result -> {
          try {
            DBTestUtils.executeSQLQuery(
              sqlQuery,
              postgresContainer);
          } catch (SQLException e) {
            context.failNow(e);
          }
        }
      );
  }

  public static void executeSQLQueryFromResource(String sqlResource,
      PostgreSQLContainer<?> postgresContainer) throws IOException, SQLException {
    executeSQLQueryFromResource(sqlResource, postgresContainer.getJdbcUrl(),
        postgresContainer.getUsername(), postgresContainer.getPassword());
  }

  public static DatasourceConfig datasourceConfig(String jdbcUrl, String database, String username,
      String password) {
    int postgresPort = Integer.parseInt(
        StringUtils.substringBetween(jdbcUrl, "localhost:", "/")
    );

    DatasourceConfig datasourceConfig = new DatasourceConfig();
    datasourceConfig.setHost("localhost");
    datasourceConfig.setPort(postgresPort);
    datasourceConfig.setDb(database);
    datasourceConfig.setUsername(username);
    datasourceConfig.setPassword(password);

    return datasourceConfig;
  }

  public static DatasourceConfig datasourceConfig(PostgreSQLContainer<?> postgresContainer) {
    return datasourceConfig(
        postgresContainer.getJdbcUrl(),
        postgresContainer.getDatabaseName(),
        postgresContainer.getUsername(),
        postgresContainer.getPassword()
    );
  }
}
