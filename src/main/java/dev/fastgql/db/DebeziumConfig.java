package dev.fastgql.db;

import io.vertx.core.json.JsonObject;

public class DebeziumConfig {
  private final boolean active;
  private final boolean embedded;
  private final String bootstrapServers;
  private final String serverName;

  private DebeziumConfig() {
    this.active = false;
    this.embedded = false;
    this.bootstrapServers = null;
    this.serverName = null;
  }

  private DebeziumConfig(boolean embedded, String bootstrapServers, String serverName) {
    this.active = true;
    this.embedded = embedded;
    this.bootstrapServers = bootstrapServers;
    this.serverName = serverName;
  }

  public static DebeziumConfig createWithJsonConfig(JsonObject config) {
    if (config != null) {
      return new DebeziumConfig(
          config.getBoolean("embedded"),
          config.getString("bootstrap.servers"),
          config.getString("server"));
    } else {
      return new DebeziumConfig();
    }
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public String getServerName() {
    return serverName;
  }

  public boolean isEmbedded() {
    return embedded;
  }

  public boolean isActive() {
    return active;
  }
}
