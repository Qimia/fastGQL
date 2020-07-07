package dev.fastgql.integration;

import dev.fastgql.FastGQL;
import dev.fastgql.db.DatasourceConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.lifecycle.Startables;

public abstract class AbstractContainerEnvImpl implements AbstractContainerEnv {
  private final Logger log = LoggerFactory.getLogger(AbstractContainerEnvImpl.class);
  // todo: random port?
  protected final int port = 8081;
  protected final JdbcDatabaseContainer<?> jdbcDatabaseContainer = createJdbcContainer();
  protected String deploymentID;

  protected abstract JdbcDatabaseContainer<?> createJdbcContainer();

  protected abstract DatasourceConfig createDatasourceConfig();

  public abstract String getJdbcUrlForMultipleQueries();

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
    Startables.deepStart(Stream.of(jdbcDatabaseContainer)).join();

    DatasourceConfig datasourceConfig = createDatasourceConfig();

    JsonObject config =
        new JsonObject()
            .put("http.port", port)
            .put("debezium", Map.of("embedded", true, "server", "dbserver"))
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
            });
  }

  @Override
  public void tearDown(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(deploymentID)
        .subscribe(
            () -> {
              jdbcDatabaseContainer.close();
              context.completeNow();
            });
  }
}
