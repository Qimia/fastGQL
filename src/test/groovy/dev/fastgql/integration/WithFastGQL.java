package dev.fastgql.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.VertxModule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import java.util.Map;
import java.util.stream.Stream;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.lifecycle.Startables;

public interface WithFastGQL {
  String getDeploymentID();

  void setDeploymentID(String deploymentID);

  JdbcDatabaseContainer<?> getJdbcDatabaseContainer();

  JdbcDatabaseContainer<?> createJdbcDatabaseContainerWithoutNetwork();

  DatasourceConfig createDatasourceConfig();

  String getJdbcUrlForMultipleQueries();

  default Network getNetwork() {
    return null;
  }

  default int getDeploymentPort() {
    return 8081;
  }

  default boolean isDebeziumActive() {
    return false;
  }

  default boolean isSecurityActive() {
    return false;
  }

  default JdbcDatabaseContainer<?> createJdbcDatabaseContainer() {
    if (getNetwork() != null) {
      return createJdbcDatabaseContainerWithoutNetwork().withNetwork(getNetwork());
    } else {
      return createJdbcDatabaseContainerWithoutNetwork();
    }
  }

  default Map<String, Object> createDebeziumConfigEntry() {
    return Map.of();
  }

  default Map<String, Object> createSecurityConfigEntry() {
    return Map.of();
  }

  default String getJwtToken(Vertx vertx, Map<String, Object> userParams) {
    return null;
  }

  default Stream<GenericContainer<?>> getAllContainers() {
    return Stream.of(getJdbcDatabaseContainer());
  }

  default void closeAllContainers() {
    getJdbcDatabaseContainer().close();
  }

  default JsonObject createConfigMultipleQueries() {
    DatasourceConfig datasourceConfig = createDatasourceConfig();
    JsonObject config =
        new JsonObject()
            .put("http.port", getDeploymentPort())
            .put(
                "datasource",
                Map.of(
                    "jdbcUrl", getJdbcUrlForMultipleQueries(),
                    "username", datasourceConfig.getUsername(),
                    "password", datasourceConfig.getPassword(),
                    "schema", datasourceConfig.getSchema()));

    if (isDebeziumActive()) {
      config.put("debezium", createDebeziumConfigEntry());
    }

    if (isSecurityActive()) {
      config.put("auth", createSecurityConfigEntry());
    }

    return config;
  }

  default JsonObject createConfig() {
    DatasourceConfig datasourceConfig = createDatasourceConfig();
    JsonObject config =
        new JsonObject()
            .put("http.port", getDeploymentPort())
            .put(
                "datasource",
                Map.of(
                    "jdbcUrl", datasourceConfig.getJdbcUrl(),
                    "username", datasourceConfig.getUsername(),
                    "password", datasourceConfig.getPassword(),
                    "schema", datasourceConfig.getSchema()));

    if (isDebeziumActive()) {
      config.put("debezium", createDebeziumConfigEntry());
    }

    if (isSecurityActive()) {
      config.put("auth", createSecurityConfigEntry());
    }

    return config;
  }

  default boolean registerConnector(VertxTestContext context) {
    return true;
  }

  default void setup(Vertx vertx, VertxTestContext context, AbstractVerticle verticle) {
    Startables.deepStart(getAllContainers()).join();
    if (!registerConnector(context)) {
      return;
    }
    DeploymentOptions options = new DeploymentOptions().setConfig(createConfig());
    vertx
        .rxDeployVerticle(verticle, options)
        .subscribe(
            deploymentID -> {
              setDeploymentID(deploymentID);
              context.completeNow();
            });
  }

  default void tearDown(Vertx vertx, VertxTestContext context) {
    vertx
        .rxUndeploy(getDeploymentID())
        .subscribe(
            () -> {
              closeAllContainers();
              context.completeNow();
            });
  }

  default Pool getPool(Vertx vertx) {
    Injector injector =
        Guice.createInjector(new VertxModule(vertx, createConfig()), new DatabaseModule());
    return injector.getInstance(Pool.class);
  }

  default Pool getPoolMultipleQueries(Vertx vertx) {
    Injector injector =
        Guice.createInjector(
            new VertxModule(vertx, createConfigMultipleQueries()), new DatabaseModule());
    return injector.getInstance(Pool.class);
  }
}
