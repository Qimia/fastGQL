package ai.qimia.vertxtest;

import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtils {
  public static void initializeDB(String sqlResource, String jdbcUrl,
                                  String username, String password) throws SQLException, IOException {
    String initSQL = GraphQLTestUtils.readResource(sqlResource);
    try (
      Connection connection = DriverManager.getConnection(
        jdbcUrl,
        username,
        password
      );
      Statement statement = connection.createStatement()
    ) {
      statement.execute(initSQL);
    }
  }

  public static DatasourceConfig datasourceConfig(String jdbcUrl, String database, String username, String password) {
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
}
