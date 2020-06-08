/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.configuration;

import io.vertx.core.json.JsonObject;

public class DatasourceConfig {

    final private String host;
    final private Integer port;
    final private String db;
    final private String username;
    final private String password;

    public DatasourceConfig(JsonObject config) {
        this.host = config.getJsonObject("datasource").getString("host");
        this.port = config.getJsonObject("datasource").getInteger("port");
        this.db = config.getJsonObject("datasource").getString("db");
        this.username = config.getJsonObject("datasource").getString("username");
        this.password = config.getJsonObject("datasource").getString("password");
    }

    public String getConnectionUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
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
