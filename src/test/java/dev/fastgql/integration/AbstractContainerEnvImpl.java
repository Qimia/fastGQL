package dev.fastgql.integration;

import dev.fastgql.FastGQL;
import dev.fastgql.db.DatasourceConfig;
import io.debezium.testing.testcontainers.ConnectorConfiguration;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;

public abstract class AbstractContainerEnvImpl implements AbstractContainerEnv {
  private final Logger LOGGER = LoggerFactory.getLogger(AbstractContainerEnvImpl.class);
  // todo: random port?
  private final int port = 8081;
  protected final Network network = Network.newNetwork();
  protected final KafkaContainer kafkaContainer = new KafkaContainer().withNetwork(network);
  protected final JdbcDatabaseContainer<?> jdbcDatabaseContainer = createJdbcContainer();
  private final DebeziumContainer debeziumContainer =
      new DebeziumContainer("1.0")
          .withNetwork(network)
          .withKafka(kafkaContainer)
          .withLogConsumer(new Slf4jLogConsumer(LOGGER))
          .dependsOn(kafkaContainer);
  private String deploymentID;

  protected abstract JdbcDatabaseContainer<?> createJdbcContainer();

  protected abstract ConnectorConfiguration createConnectorConfiguration();

  protected abstract DatasourceConfig createDatasourceConfig();

  protected abstract String getJdbcUrlForMultipleQueries();

  @Override
  public int getDeploymentPort() {
    return port;
  }

  @Override
  public JdbcDatabaseContainer<?> getJdbcDatabaseContainer() {
    return jdbcDatabaseContainer;
  }

  @Override
  public void setup(Vertx vertx, VertxTestContext context) {
    Startables.deepStart(Stream.of(kafkaContainer, jdbcDatabaseContainer, debeziumContainer))
        .join();

    try {
      DBTestUtils.executeSQLQueryFromResource(
          "init.sql",
          getJdbcUrlForMultipleQueries(),
          jdbcDatabaseContainer.getUsername(),
          jdbcDatabaseContainer.getPassword());
    } catch (SQLException | IOException e) {
      context.failNow(e);
      return;
    }

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
            .put("bootstrap.servers", kafkaContainer.getBootstrapServers())
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
        .doOnSuccess(
            deploymentID -> {
              this.deploymentID = deploymentID;
              context.completeNow();
            })
        .subscribe();
  }

  @Override
  public void tearDown(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(deploymentID)
        .doOnComplete(
            () -> {
              debeziumContainer.close();
              kafkaContainer.close();
              jdbcDatabaseContainer.close();
              network.close();
              context.completeNow();
            })
        .subscribe();
  }
}
