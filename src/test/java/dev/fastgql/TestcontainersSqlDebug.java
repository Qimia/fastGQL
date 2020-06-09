package dev.fastgql;

import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

public class TestcontainersSqlDebug {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestcontainersSqlDebug.class);
  public static void main(String[] args) throws SQLException, IOException {
    Network network = Network.newNetwork();
    KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);
    MySQLContainer<?> mysqlContainer = new MySQLContainer<>("fastgql/mysql-testcontainers:latest");

    DebeziumContainer debeziumContainer =
      new DebeziumContainer("1.0")
        .withNetwork(network)
        .withKafka(kafkaContainer)
        .withLogConsumer(new Slf4jLogConsumer(LOGGER))
        .dependsOn(kafkaContainer);

    Startables.deepStart(Stream.of(kafkaContainer, mysqlContainer, debeziumContainer)).join();

    try (Connection connection = DriverManager.getConnection(mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(), mysqlContainer.getPassword());
         Statement statement = connection.createStatement()) {
      statement.execute("SELECT 1");
    }

    debeziumContainer.registerConnector(
      "my-connector",
      ConnectorConfiguration.forJdbcContainer(mysqlContainer)
        .with("database.server.name", "dbserver")
        .with("slot.name", "debezium"));
  }
}
