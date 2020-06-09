package dev.fastgql.db;

import io.vertx.core.json.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Datasource config.
 *
 * @author Kamil Bobrowski
 */
public class DatasourceConfig {
  private static final Logger LOGGER = Logger.getLogger(DatasourceConfig.class.getName());

  private final String host;
  private final int port;
  private final String db;
  private final String username;
  private final String password;

  /**
   * Create datasource from json config.
   *
   * @param config json config
   */
  public DatasourceConfig(JsonObject config) {
    this.host = config.getString("host");
    this.port = config.getInteger("port");
    this.db = config.getString("db");
    this.username = config.getString("username");
    this.password = config.getString("password");
  }

  public static DatasourceConfig createDatasourceConfig(JsonObject config) {
    final String msg = "Missing value for datasource.";
    DatasourceConfig datasourceConfig = new DatasourceConfig(config);

    try {
      if (datasourceConfig.getHost() == null) {throw new NullPointerException(String.format("%shost!", msg));}
      if (datasourceConfig.getPort() == 0) {throw new NullPointerException(String.format("%sport!", msg));}
      if (datasourceConfig.getDb() == null) {throw new NullPointerException(String.format("%sdb!", msg));}
      if (datasourceConfig.getUsername() == null) {throw new NullPointerException(String.format("%susername!", msg));}
      if (datasourceConfig.getPassword() == null) {throw new NullPointerException(String.format("%spassword!", msg));}
    } catch (NullPointerException npe) {
      LOGGER.log(Level.SEVERE, npe.toString());
    }

    return datasourceConfig;
  }

  public String getJdbcUrl() {
    return String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
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
