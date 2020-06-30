/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

public class EmbeddedDebeziumTest {
  private static final Network network = Network.newNetwork();
  public static PostgreSQLContainer<?> postgreSQLContainer =
      new PostgreSQLContainer<>("debezium/postgres:11")
          .withNetwork(network)
          .withNetworkAliases("postgres");

  @BeforeAll
  public static void startContainers() {
    Startables.deepStart(Stream.of(postgreSQLContainer)).join();
  }

  @Test
  public void canConnectPostgreSql() {
    Properties props = setEngineProperties(postgreSQLContainer);
    try (Connection connection = getConnection(postgreSQLContainer);
        Statement statement = connection.createStatement();
        DebeziumEngine<ChangeEvent<String, String>> engine =
            DebeziumEngine.create(Json.class).using(props).notifying(System.out::println).build()) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      executor.execute(engine);
      statement.execute("create schema todo");
      statement.execute(
          "create table todo.Todo (id int not null, title varchar(255), primary key (id))");
      statement.execute("alter table todo.Todo replica identity full");
      statement.execute("insert into todo.Todo values (1, 'Learn CDC')");
      statement.execute("insert into todo.Todo values (2, 'Learn Debezium')");
      Thread.sleep(5000);
      executor.shutdown();
    } catch (SQLException | IOException | InterruptedException throwables) {
      throwables.printStackTrace();
    }
  }

  private Connection getConnection(PostgreSQLContainer<?> postgreSQLContainer) throws SQLException {
    return DriverManager.getConnection(
        postgreSQLContainer.getJdbcUrl(),
        postgreSQLContainer.getUsername(),
        postgreSQLContainer.getPassword());
  }

  private Properties setEngineProperties(PostgreSQLContainer<?> postgreSQLContainer) {
    Properties props = new Properties();
    props.setProperty("name", "engine");
    props.setProperty("connector.class", "io.debezium.connector.postgresql.PostgresConnector");
    props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
    props.setProperty("offset.storage.file.filename", setOffsetStorageFilename());
    props.setProperty("offset.flush.interval.ms", "1000");

    props.setProperty("database.hostname", "localhost");
    props.setProperty("database.port", Integer.toString(postgreSQLContainer.getMappedPort(5432)));
    props.setProperty("database.user", postgreSQLContainer.getUsername());
    props.setProperty("database.password", postgreSQLContainer.getPassword());
    props.setProperty("database.dbname", postgreSQLContainer.getDatabaseName());
    props.setProperty("database.server.name", "dbserver");
    return props;
  }

  private String setOffsetStorageFilename() {
    Random random = new Random();
    return String.format("/tmp/offsets-%d.dat", random.ints().findFirst().getAsInt());
  }
}
