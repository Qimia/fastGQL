package dev.fastgql.integration;

import java.util.Map;

public interface WithEmbeddedDebezium extends WithDebezium {
  @Override
  default Map<String, Object> createDebeziumConfigEntry() {
    return Map.of("embedded", true, "server", "dbserver");
  }
}
