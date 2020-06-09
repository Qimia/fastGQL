package dev.fastgql.db;

import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Datasource config.
 *
 * @author Kamil Bobrowski
 */
public class DatasourceConfig {
  private enum DBType {
    postgresql,
    mysql,
    other
  }

  private final String jdbcUrl;
  private final DBType dbType;
  private final String host;
  private final int port;
  private final String db;
  private final String username;
  private final String password;

  private DatasourceConfig(
      String jdbcUrl,
      DBType dbType,
      String host,
      int port,
      String db,
      String username,
      String password) {
    this.jdbcUrl = jdbcUrl;
    this.dbType = dbType;
    this.host = host;
    this.port = port;
    this.db = db;
    this.username = username;
    this.password = password;
  }

  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, username, password);
  }

  /**
   * Generate {@link Pool} of proper type.
   *
   * @param vertx vertx instance
   * @return new Pool
   */
  public Pool getPool(Vertx vertx) {
    switch (dbType) {
      case mysql:
        return MySQLPool.pool(
            vertx,
            new MySQLConnectOptions()
                .setHost(host)
                .setPort(port)
                .setDatabase(db)
                .setUser(username)
                .setPassword(password),
            new PoolOptions().setMaxSize(5));
      case postgresql:
        return PgPool.pool(
            vertx,
            new PgConnectOptions()
                .setHost(host)
                .setPort(port)
                .setDatabase(db)
                .setUser(username)
                .setPassword(password),
            new PoolOptions().setMaxSize(5));
      case other:
      default:
        throw new RuntimeException("Cannot generate Pool for unsupported database type");
    }
  }

  /**
   * Standard create method.
   *
   * @param jdbcUrl JDBC url
   * @param username database user name
   * @param password database password
   */
  public static DatasourceConfig createDatasourceConfig(
      String jdbcUrl, String username, String password) {
    Pattern pattern = Pattern.compile("jdbc:(postgresql|mysql)://(\\w+):(\\d{4,5})/(\\w+)\\??\\S*");
    Matcher matcher = pattern.matcher(jdbcUrl);
    if (matcher.matches()) {
      String dbTypeString = matcher.group(1);
      DBType dbType = DBType.other;
      if (dbTypeString.equals("postgresql")) {
        dbType = DBType.postgresql;
      } else if (dbTypeString.equals("mysql")) {
        dbType = DBType.mysql;
      }
      if (dbType == DBType.other) {
        throw new IllegalArgumentException("Unsupported DB type");
      }
      String host = matcher.group(2);
      int port = Integer.parseInt(matcher.group(3));
      String db = matcher.group(4);
      return new DatasourceConfig(jdbcUrl, dbType, host, port, db, username, password);
    } else {
      throw new IllegalArgumentException("JDBC url not valid");
    }
  }

  /**
   * Create datasource from json config.
   *
   * @param config json config
   */
  public static DatasourceConfig createWithJsonConfig(JsonObject config) {
    return createDatasourceConfig(
        config.getString("jdbcUrl"), config.getString("username"), config.getString("password"));
  }

  public String getJdbcUrl() {
    return jdbcUrl;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getDb() {
    return db;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
