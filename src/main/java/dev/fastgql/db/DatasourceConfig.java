/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import io.vertx.core.json.JsonObject;

/**
 * Datasource config.
 *
 * @author Kamil Bobrowski
 */
public class DatasourceConfig {
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
    this.host = host;
    this.port = port;
    this.db = db;
    this.username = username;
    this.password = password;
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


