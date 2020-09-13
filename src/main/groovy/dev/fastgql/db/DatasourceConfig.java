/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import dev.fastgql.sql.TableAlias;
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
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Datasource config.
 *
 * @author Kamil Bobrowski
 */
public class DatasourceConfig {
  public enum DBType {
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
  private final String schema;

  private DatasourceConfig(
      String jdbcUrl,
      DBType dbType,
      String host,
      int port,
      String db,
      String schema,
      String username,
      String password) {
    this.jdbcUrl = jdbcUrl;
    this.dbType = dbType;
    this.host = host;
    this.port = port;
    this.db = db;
    this.schema = schema;
    this.username = username;
    this.password = password;
  }

  private static class JdbcParseResult {
    private final DBType dbType;
    private final String host;
    private final int port;
    private final String db;

    private JdbcParseResult(DBType dbType, String host, int port, String db) {
      this.dbType = dbType;
      this.host = host;
      this.port = port;
      this.db = db;
    }

    private static JdbcParseResult parse(String jdbcUrl) {
      Pattern pattern =
          Pattern.compile("jdbc:(postgresql|mysql)://(\\w+):(\\d{4,5})/(\\w+)\\??\\S*");
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
        return new JdbcParseResult(dbType, host, port, db);
      } else {
        throw new IllegalArgumentException("JDBC url not valid");
      }
    }
  }

  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(jdbcUrl, username, password);
  }

  public String getUnlockQuery() {
    switch (dbType) {
      case mysql:
        return "UNLOCK TABLES";
      case postgresql:
      case other:
      default:
        return null;
    }
  }

  public Function<Set<TableAlias>, String> tableListLockQueryFunction() {
    switch (dbType) {
      case mysql:
        return tables ->
            String.format(
                "LOCK TABLES %s",
                tables.stream()
                    .map(
                        table ->
                            String.format(
                                "%s as %s READ", table.getTableName(), table.getTableAlias()))
                    .collect(Collectors.joining(", ")));
      case postgresql:
        return tables ->
            String.format(
                "LOCK TABLE %s IN SHARE MODE",
                tables.stream()
                    .map(TableAlias::getTableName)
                    .distinct()
                    .collect(Collectors.joining(", ")));
      case other:
      default:
        return tableWithAliases -> null;
    }
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
    JdbcParseResult result = JdbcParseResult.parse(jdbcUrl);
    return new DatasourceConfig(
        jdbcUrl, result.dbType, result.host, result.port, result.db, result.db, username, password);
  }

  /**
   * Create method with custom schema.
   *
   * @param jdbcUrl JDBC url
   * @param username database user name
   * @param password database password
   * @param schema database schema
   */
  public static DatasourceConfig createDatasourceConfig(
      String jdbcUrl, String username, String password, String schema) {
    JdbcParseResult result = JdbcParseResult.parse(jdbcUrl);
    return new DatasourceConfig(
        jdbcUrl, result.dbType, result.host, result.port, result.db, schema, username, password);
  }

  /**
   * Create datasource from json config.
   *
   * @param config json config
   */
  public static DatasourceConfig createWithJsonConfig(JsonObject config) {
    return createDatasourceConfig(
        config.getString("jdbcUrl"),
        config.getString("username"),
        config.getString("password"),
        config.getString("schema"));
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

  public String getSchema() {
    return schema;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public DBType getDbType() {
    return dbType;
  }
}
