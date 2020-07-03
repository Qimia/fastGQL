package dev.fastgql.events;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DebeziumEngineSingleton {

  private static final Logger log = Logger.getLogger(DebeziumEngineSingleton.class);

  private DebeziumEngineSingleton() {}

  private static final Subject<ChangeEvent<String, String>> changeEventSubject = BehaviorSubject.<ChangeEvent<String, String>>create().toSerialized();
  private static DebeziumEngine<ChangeEvent<String, String>> debeziumEngine;
  private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

  public static synchronized void startNewEngine(DatasourceConfig datasourceConfig, DebeziumConfig debeziumConfig) throws IOException {
    stopEngine();

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

    debeziumEngine = DebeziumEngine.create(Json.class).using(props).notifying(changeEventSubject::onNext).build();
    log.debug("starting debezium engine");
    executorService.execute(debeziumEngine);
  }

  public static synchronized void stopEngine() throws IOException {
    if (debeziumEngine != null) {
      log.debug("closing debezium engine");
      debeziumEngine.close();
      debeziumEngine = null;
    }
  }

  public static Flowable<ChangeEvent<String, String>> getChangeEventFlowable() {
    return changeEventSubject.toFlowable(BackpressureStrategy.BUFFER);
  }
}
