/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.kafka;

import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 * Kafka config.
 *
 * @author Martin Biolik
 */
public class KafkaConfig {
  private static final Logger LOGGER = Logger.getLogger(KafkaConfig.class.getName());

  private String bootstrapServers;
  private String keyDeserializer;
  private String valueDeserializer;
  private String autoOffsetReset;
  private final String msg = "Missing value for ";

  /**
   * Create kafka config from json config.
   *
   * @param config json config
   */
  public KafkaConfig(JsonObject config) {
    try {
      if (config.getString("bootstrap.servers") == null) {
        throw new NullPointerException(String.format("%sbootstrap.servers!", msg));
      } else {
        this.bootstrapServers = config.getString("bootstrap.servers");
      }

      if (config.getJsonObject("kafka").getString("key.deserializer") == null) {
        throw new NullPointerException(String.format("%skafka.key.deserializer!", msg));
      } else {
        this.keyDeserializer = config.getJsonObject("kafka").getString("key.deserializer");
      }

      if (config.getJsonObject("kafka").getString("value.deserializer") == null) {
        throw new NullPointerException(String.format("%skafka.value.deserializer!", msg));
      } else {
        this.valueDeserializer = config.getJsonObject("kafka").getString("value.deserializer");
      }

      if (config.getJsonObject("kafka").getString("auto.offset.reset") == null) {
        throw new NullPointerException(String.format("%skafka.auto.offset.reset!", msg));
      } else {
        this.autoOffsetReset = config.getJsonObject("kafka").getString("auto.offset.reset");
      }
    } catch (NullPointerException npe) {
      LOGGER.log(Level.SEVERE, npe.toString());
    }
  }

  /**
   * Standard constructor.
   *
   * @param bootstrapServers host (e.g. "http://localhost:9092")
   */
  public KafkaConfig(
      String bootstrapServers,
      String keyDeserializer,
      String valueDeserializer,
      String autoOffsetReset) {
    try {
      if (bootstrapServers == null) {
        throw new NullPointerException(String.format("%sbootstrap.servers!", msg));
      } else {
        this.bootstrapServers = bootstrapServers;
      }

      if (keyDeserializer == null) {
        throw new NullPointerException(String.format("%skafka.key.deserializer!", msg));
      } else {
        this.keyDeserializer = keyDeserializer;
      }

      if (valueDeserializer == null) {
        throw new NullPointerException(String.format("%skafka.value.deserializer!", msg));
      } else {
        this.valueDeserializer = valueDeserializer;
      }

      if (autoOffsetReset == null) {
        throw new NullPointerException(String.format("%skafka.auto.offset.reset!", msg));
      } else {
        this.autoOffsetReset = autoOffsetReset;
      }
    } catch (NullPointerException npe) {
      LOGGER.log(Level.SEVERE, npe.toString());
    }
  }

  public Map<String, String> createConfigMap() {
    Map<String, String> configMap = new HashMap<>();

    configMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
    configMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getKeyDeserializer());
    configMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
    configMap.put(ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID());
    configMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

    return configMap;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public void setBootstrapServers(String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
  }

  public String getKeyDeserializer() {
    return keyDeserializer;
  }

  public void setKeyDeserializer(String keyDeserializer) {
    this.keyDeserializer = keyDeserializer;
  }

  public String getValueDeserializer() {
    return valueDeserializer;
  }

  public void setValueDeserializer(String valueDeserializer) {
    this.valueDeserializer = valueDeserializer;
  }

  public String getAutoOffsetReset() {
    return autoOffsetReset;
  }

  public void setAutoOffsetReset(String autoOffsetReset) {
    this.autoOffsetReset = autoOffsetReset;
  }
}
