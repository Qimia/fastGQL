package dev.fastgql.kafka;

import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 * Utils for creating Kafka consumer.
 *
 * @author Kamil Bobrowski
 */
public class KafkaConsumerUtils {

  /**
   * Create Kafka consumer and subscribe to given topics.
   *
   * @param topics set of topics to subscribe
   * @param bootstrapServers Kafka bootstrap servers
   * @param vertx vertx instance
   * @return new Kafka consumer
   */
  public static KafkaConsumer<String, String> createForTopics(
      Set<String> topics, String bootstrapServers, Vertx vertx) {
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    kafkaConfig.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    kafkaConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "tc-" + UUID.randomUUID());
    kafkaConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, kafkaConfig);
    consumer.subscribe(topics);
    return consumer;
  }
}
