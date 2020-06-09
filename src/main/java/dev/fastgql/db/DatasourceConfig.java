package dev.fastgql.db;

import dev.fastgql.kafka.KafkaConfig;
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

  private String host;
  private int port;
  private String db;
  private String username;
  private String password;
  private final String msg = "Missing value for datasource.";

  /**
   * Create datasource from json config.
   *
   * @param config json config
   */
  public DatasourceConfig(JsonObject config) {

    try {
      if(config.getString("host") == null) {
        throw new NullPointerException(String.format("%shost!", msg));
      } else {
        this.host = config.getString("host");
      }

      if(config.getInteger("port") == null) {
        throw new NullPointerException(String.format("%sport!", msg));
      } else {
        this.port = config.getInteger("port");
      }

      if(config.getString("db") == null) {
        throw new NullPointerException(String.format("%sdb!", msg));
      } else {
        this.db = config.getString("db");
      }

      if(config.getString("username") == null) {
        throw new NullPointerException(String.format("%susername!", msg));
      } else {
        this.username = config.getString("username");
      }

      if(config.getString("password") == null) {
        throw new NullPointerException(String.format("%spassword!", msg));
      } else {
        this.password = config.getString("password");
      }
    } catch (NullPointerException npe) {
      LOGGER.log(Level.SEVERE, npe.toString());
    }
  }

  /**
   * Standard constructor.
   *
   * @param host host (e.g. "localhost")
   * @param port database port (e.g. 5432)
   * @param db database name
   * @param username database user name
   * @param password database password
   */
  public DatasourceConfig(String host, int port, String db, String username, String password) {
    try {
      if(host == null) {
        throw new NullPointerException(String.format("%shost!", msg));
      } else {
        this.host = host;
      }

      if(port == 0) {
        throw new NullPointerException(String.format("%sport!", msg));
      } else {
        this.port = port;
      }

      if(db == null) {
        throw new NullPointerException(String.format("%sdb!", msg));
      } else {
        this.db = db;
      }

      if(username == null) {
        throw new NullPointerException(String.format("%susername!", msg));
      } else {
        this.username = username;
      }

      if(password == null) {
        throw new NullPointerException(String.format("%spassword!", msg));
      } else {
        this.password = password;
      }
    } catch (NullPointerException npe) {
      LOGGER.log(Level.SEVERE, npe.toString());
    }
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
