package dev.fastgql.integration;

import dev.fastgql.FastGQL;
import dev.fastgql.db.DatasourceConfig;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractContainerEnvWithDebeziumImpl extends AbstractContainerEnvImpl implements AbstractContainerEnv {
  private final Logger log = LoggerFactory.getLogger(AbstractContainerEnvWithDebeziumImpl.class);
  protected final Network network = Network.newNetwork();
  protected final KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);
  private final DebeziumContainer debeziumContainer =
      new DebeziumContainer("debezium/connect:1.2.0.Final")
          .withNetwork(network)
          .withKafka(kafkaContainer)
          .withLogConsumer(new Slf4jLogConsumer(log))
          .dependsOn(kafkaContainer);

  protected abstract ConnectorConfiguration createConnectorConfiguration();

  @Override
  public void setup(Vertx vertx, VertxTestContext context) {
    Startables.deepStart(Stream.of(kafkaContainer, jdbcDatabaseContainer, debeziumContainer))
        .join();

    try {
      debeziumContainer.registerConnector("my-connector", createConnectorConfiguration());
    } catch (IOException e) {
      context.failNow(e);
      return;
    }

    DatasourceConfig datasourceConfig = createDatasourceConfig();

    JsonObject config =
        new JsonObject()
            .put("http.port", port)
            .put(
                "debezium",
                Map.of(
                    "embedded",
                    false,
                    "bootstrap.servers",
                    kafkaContainer.getBootstrapServers(),
                    "server",
                    "dbserver"))
            .put(
                "datasource",
                Map.of(
                    "jdbcUrl", datasourceConfig.getJdbcUrl(),
                    "username", datasourceConfig.getUsername(),
                    "password", datasourceConfig.getPassword(),
                    "schema", datasourceConfig.getSchema()));

    DeploymentOptions options = new DeploymentOptions().setConfig(config);
    vertx
        .rxDeployVerticle(new FastGQL(), options)
        .subscribe(
      deploymentID -> {
        this.deploymentID = deploymentID;
        context.completeNow();
      }
        );
  }

  @Override
  public void tearDown(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(deploymentID)
        .subscribe(

      () -> {
        debeziumContainer.close();
        kafkaContainer.close();
        jdbcDatabaseContainer.close();
        network.close();
        context.completeNow();
      }
        );
  }
}
