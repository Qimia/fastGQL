/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.kafka;

import io.vertx.core.json.JsonObject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka config.
 *
 * @author Martin Biolik
 */
public class KafkaConfig {
    private String bootstrapServers;
    private String keyDeserializer;
    private String valueDeserializer;
    private String autoOffsetReset;

    /**
     * Create kafka config from json config.
     *
     * @param config json config
     */
    public KafkaConfig(JsonObject config) {
        this.bootstrapServers = config.getString("bootstrap.servers");
        this.keyDeserializer = config.getString("key.deserializer");
        this.valueDeserializer = config.getString("value.deserializer");
        this.autoOffsetReset = config.getString("auto.offset.reset");
    }

    /**
     * Standard constructor.
     *
     * @param bootstrapServers host (e.g. "http://localhost:9092")
     */
    public KafkaConfig(String bootstrapServers, String keyDeserializer, String valueDeserializer, String autoOffsetReset) {
        this.bootstrapServers = bootstrapServers;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.autoOffsetReset = autoOffsetReset;
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
