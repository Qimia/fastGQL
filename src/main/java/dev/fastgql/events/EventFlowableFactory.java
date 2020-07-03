package dev.fastgql.events;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.sql.ComponentExecutable;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumer;
import io.vertx.reactivex.kafka.client.consumer.KafkaConsumerRecord;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
      return createForEmbedded(topics, datasourceConfig, debeziumConfig);
    } else {
      return createForKafka(topics, debeziumConfig, vertx);
    }
  }

  private static Flowable<ChangeEvent<String, String>> createForEmbedded(
      Set<String> topics, DatasourceConfig datasourceConfig, DebeziumConfig debeziumConfig) {
    Random random = new Random();
    Properties props = new Properties();
    props.setProperty("name", "engine");
    props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
    props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
    props.setProperty(
        "offset.storage.file.filename",
        String.format("/tmp/offsets-%d.dat", random.ints().findFirst().getAsInt()));
    props.setProperty("offset.flush.interval.ms", "1000");

    props.setProperty("database.hostname", datasourceConfig.getHost());
    props.setProperty("database.port", Integer.toString(datasourceConfig.getPort()));
    props.setProperty("database.user", datasourceConfig.getUsername());
    props.setProperty("database.password", datasourceConfig.getPassword());
    props.setProperty("database.dbname", datasourceConfig.getDb());
    props.setProperty("database.server.name", debeziumConfig.getServerName());

    Flowable<ChangeEvent<String, String>> eventFlowable =
        Flowable.create(
            emitter -> {
              DebeziumEngine<ChangeEvent<String, String>> engine =
                  DebeziumEngine.create(Json.class).using(props).notifying(emitter::onNext).build();
              ExecutorService executor = Executors.newSingleThreadExecutor();
              executor.execute(engine);
            },
            BackpressureStrategy.BUFFER);

    return eventFlowable.filter(changeEvent -> topics.contains(changeEvent.destination()));
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
