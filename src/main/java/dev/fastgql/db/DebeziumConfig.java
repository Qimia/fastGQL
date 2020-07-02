package dev.fastgql.db;

import io.vertx.core.json.JsonObject;

public class DebeziumConfig {
  private final boolean embedded;
  private final String bootstrapServers;
  private final String serverName;

  private DebeziumConfig(boolean embedded, String bootstrapServers, String serverName) {
    this.embedded = embedded;
    this.bootstrapServers = bootstrapServers;
    this.serverName = serverName;
  }

  public static DebeziumConfig createWithJsonConfig(JsonObject config) {
    return new DebeziumConfig(
        config.getBoolean("embedded"),
        config.getString("bootstrap.servers"),
        config.getString("server"));
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
}
