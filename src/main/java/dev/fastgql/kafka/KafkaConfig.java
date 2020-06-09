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

  private final String bootstrapServers;
  private final String keyDeserializer;
  private final String valueDeserializer;
  private final String autoOffsetReset;

  /**
   * Create kafka config from json config.
   *
   * @param config json config
   */
  private KafkaConfig(JsonObject config) {
    this.bootstrapServers = config.getString("bootstrap.servers");
    this.keyDeserializer = config.getJsonObject("kafka").getString("key.deserializer");
    this.valueDeserializer = config.getJsonObject("kafka").getString("value.deserializer");
    this.autoOffsetReset = config.getJsonObject("kafka").getString("auto.offset.reset");
  }

  public static Map<String, String> createConfigMap(JsonObject config) {
    final String msg = "Missing value for ";
    KafkaConfig kafkaConfig = new KafkaConfig(config);

    try {
      if (kafkaConfig.getBootstrapServers() == null) {throw new NullPointerException(String.format("%sbootstrap.servers!", msg));}
      if (kafkaConfig.getKeyDeserializer() == null) {throw new NullPointerException(String.format("%skafka.key.deserializer!", msg));}
      if (kafkaConfig.getValueDeserializer() == null) {throw new NullPointerException(String.format("%skafka.value.deserializer!", msg));}
      if (kafkaConfig.getAutoOffsetReset() == null) {throw new NullPointerException(String.format("%skafka.auto.offset.reset", msg));}
    } catch (NullPointerException npe) {
      LOGGER.log(Level.SEVERE, npe.toString());
    }

    Map<String, String> configMap = new HashMap<>();
    configMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
    configMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, kafkaConfig.getKeyDeserializer());
    configMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, kafkaConfig.getValueDeserializer());
    configMap.put(ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID());
    configMap.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConfig.getAutoOffsetReset());

    return configMap;
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }

  public String getKeyDeserializer() {
    return keyDeserializer;
  }

  public String getValueDeserializer() {
    return valueDeserializer;
  }

  public String getAutoOffsetReset() {
    return autoOffsetReset;
  }
}
