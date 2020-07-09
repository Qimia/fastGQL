package dev.fastgql.events;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.sql.ComponentExecutable;
import io.debezium.engine.ChangeEvent;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/** Utils for creating Kafka consumer. */
public class EventFlowableFactory {

  public static Flowable<ChangeEvent<String, String>> create(
      ComponentExecutable executionRoot,
      Vertx vertx,
      DatasourceConfig datasourceConfig,
      DebeziumConfig debeziumConfig) {
    Set<String> topics =
        executionRoot.getQueriedTables().stream()
            .map(
                queriedTable ->
                    String.format(
                        "%s.%s.%s",
                        debeziumConfig.getServerName(), datasourceConfig.getSchema(), queriedTable))
            .collect(Collectors.toSet());
    if (debeziumConfig.isEmbedded()) {
      return createForEmbedded(topics);
    } else {
      return createForKafka(topics, debeziumConfig, vertx);
    }
  }

  private static Flowable<ChangeEvent<String, String>> createForEmbedded(Set<String> topics) {
    return DebeziumEngineSingleton.getChangeEventFlowable()
        .filter(changeEvent -> topics.contains(changeEvent.destination()));
  }

  private static Flowable<ChangeEvent<String, String>> createForKafka(
      Set<String> topics, DebeziumConfig debeziumConfig, Vertx vertx) {
    Map<String, String> kafkaConfig = new HashMap<>();
    kafkaConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, debeziumConfig.getBootstrapServers());
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
    return consumer
        .toFlowable()
        .map(
            new Function<>() {
              @Override
              public ChangeEvent<String, String> apply(
                  @NonNull KafkaConsumerRecord<String, String> record) {
                return new ChangeEvent<>() {
                  @Override
                  public String key() {
                    return record.key();
                  }

                  @Override
                  public String value() {
                    return record.value();
                  }

                  @Override
                  public String destination() {
                    return record.topic();
                  }
                };
              }
            });
  }
}
