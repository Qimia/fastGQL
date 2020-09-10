package dev.fastgql.integration;

import io.debezium.testing.testcontainers.DebeziumContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public abstract class ContainerEnvWithDatabaseAndDebezium implements WithExternalDebezium {
  private final Logger log = LoggerFactory.getLogger(ContainerEnvWithDatabaseAndDebezium.class);
  private final Network network = Network.newNetwork();
  private final JdbcDatabaseContainer<?> jdbcDatabaseContainer = createJdbcDatabaseContainer();
  private final KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);
  private final DebeziumContainer debeziumContainer =
      new DebeziumContainer("debezium/connect:1.2.0.Final")
          .withNetwork(network)
          .withKafka(kafkaContainer)
          .withLogConsumer(new Slf4jLogConsumer(log))
          .dependsOn(kafkaContainer);
  private String deploymentID;

  @Override
  public JdbcDatabaseContainer<?> getJdbcDatabaseContainer() {
    return jdbcDatabaseContainer;
  }

  @Override
  public DebeziumContainer getDebeziumContainer() {
    return debeziumContainer;
  }

  @Override
  public KafkaContainer getKafkaContainer() {
    return kafkaContainer;
  }

  @Override
  public Network getNetwork() {
    return network;
  }

  @Override
  public String getDeploymentID() {
    return deploymentID;
  }

  @Override
  public void setDeploymentID(String deploymentID) {
    this.deploymentID = deploymentID;
  }
}
