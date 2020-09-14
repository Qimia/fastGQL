package dev.fastgql.events;

import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import io.debezium.connector.mysql.MySqlConnector;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import io.debezium.relational.history.FileDatabaseHistory;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.log4j.Logger;

@Singleton
public class DebeziumEngineSingleton {

  private final Logger log = Logger.getLogger(DebeziumEngineSingleton.class);
  private final DatasourceConfig datasourceConfig;
  private final DebeziumConfig debeziumConfig;
  private final Subject<ChangeEvent<String, String>> changeEventSubject =
      BehaviorSubject.<ChangeEvent<String, String>>create().toSerialized();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private DebeziumEngine<ChangeEvent<String, String>> debeziumEngine;

  @Inject
  public DebeziumEngineSingleton(DatasourceConfig datasourceConfig, DebeziumConfig debeziumConfig) {
    this.datasourceConfig = datasourceConfig;
    this.debeziumConfig = debeziumConfig;
  }

  public synchronized void startNewEngine() throws IOException {
    stopEngine();

    Random random = new Random();
    Properties props = new Properties();
    props.setProperty("name", "engine");
    props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
    props.setProperty(
        "offset.storage.file.filename",
        String.format("/tmp/offsets-%d.dat", random.ints().findFirst().getAsInt()));
    props.setProperty("offset.flush.interval.ms", "1000");

    switch (datasourceConfig.getDbType()) {
      case postgresql:
        props.setProperty("connector.class", PostgresConnector.class.getName());
        props.setProperty("database.hostname", datasourceConfig.getHost());
        props.setProperty("database.port", Integer.toString(datasourceConfig.getPort()));
        props.setProperty("database.user", datasourceConfig.getUsername());
        props.setProperty("database.password", datasourceConfig.getPassword());
        props.setProperty("database.dbname", datasourceConfig.getDb());
        props.setProperty("database.server.name", debeziumConfig.getServerName());
        break;
      case mysql:
        props.setProperty("connector.class", MySqlConnector.class.getName());
        props.setProperty("database.hostname", datasourceConfig.getHost());
        props.setProperty("database.port", Integer.toString(datasourceConfig.getPort()));
        props.setProperty("database.user", datasourceConfig.getUsername());
        props.setProperty("database.password", datasourceConfig.getPassword());
        props.setProperty("database.server.id", "85744");
        props.setProperty("database.server.name", debeziumConfig.getServerName());
        props.setProperty("database.history", FileDatabaseHistory.class.getName());
        props.setProperty(
            "database.history.file.filename",
            String.format("/tmp/dbhistory-%d.dat", random.ints().findFirst().getAsInt()));
        break;
      default:
        throw new RuntimeException("DB type not supported yet");
    }

    debeziumEngine =
        DebeziumEngine.create(Json.class)
            .using(props)
            .notifying(changeEventSubject::onNext)
            .build();
    log.debug("starting debezium engine");
    executorService.execute(debeziumEngine);
  }

  public synchronized void stopEngine() throws IOException {
    if (debeziumEngine != null) {
      log.debug("closing debezium engine");
      debeziumEngine.close();
      debeziumEngine = null;
    }
  }

  public Flowable<ChangeEvent<String, String>> getChangeEventFlowable() {
    return changeEventSubject.toFlowable(BackpressureStrategy.BUFFER);
  }
}
