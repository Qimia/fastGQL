package dev.fastgql.integration;

import dev.fastgql.FastGQL;
import dev.fastgql.db.DatasourceConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.lifecycle.Startables;

import java.util.Map;
import java.util.stream.Stream;

public interface WithFastGQL {
  String getDeploymentID();
  void setDeploymentID(String deploymentID);
  JdbcDatabaseContainer<?> getJdbcDatabaseContainer();
  JdbcDatabaseContainer<?> createJdbcDatabaseContainer();
  DatasourceConfig createDatasourceConfig();
  String getJdbcUrlForMultipleQueries();

  default int getDeploymentPort() {
    return 8081;
  };

  default boolean isDebeziumActive() {
    return false;
  };

  default boolean isDebeziumEmbedded() {
    return false;
  };

  default Stream<GenericContainer<?>> getAllContainers() {
    return Stream.of(getJdbcDatabaseContainer());
  }

  default void closeAllContainers() {
    getJdbcDatabaseContainer().close();
  }

  default JsonObject createConfig() {
    DatasourceConfig datasourceConfig = createDatasourceConfig();
    JsonObject config = new JsonObject()
      .put("http.port", getDeploymentPort())
      .put(
        "datasource",
        Map.of(
          "jdbcUrl", datasourceConfig.getJdbcUrl(),
          "username", datasourceConfig.getUsername(),
          "password", datasourceConfig.getPassword(),
          "schema", datasourceConfig.getSchema()));

    if (isDebeziumActive()) {
      config.put("debezium", Map.of("embedded", isDebeziumEmbedded(), "server", "dbserver"));
    }

    return config;
  }

  default boolean registerConnector(VertxTestContext context) {
    return true;
  };

  default void setup(Vertx vertx, VertxTestContext context) {
    Startables.deepStart(getAllContainers()).join();
    if (!registerConnector(context)) {
      return;
    }
    DeploymentOptions options = new DeploymentOptions().setConfig(createConfig());
    vertx
      .rxDeployVerticle(new FastGQL(), options)
      .subscribe(
        deploymentID -> {
          setDeploymentID(deploymentID);
          context.completeNow();
        });
  }

  default void tearDown(Vertx vertx, VertxTestContext context) {
    vertx.rxUndeploy(getDeploymentID())
      .subscribe(() -> {
        closeAllContainers();
        context.completeNow();
      });
  }
}
