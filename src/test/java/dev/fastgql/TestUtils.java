/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import dev.fastgql.db.DatasourceConfig;
import io.debezium.testing.testcontainers.DebeziumContainer;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;

public class TestUtils {

  public static DeploymentOptions getDeploymentOptions(
      DatasourceConfig datasourceConfig, KafkaContainer kafkaContainer, int port) {
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
    return new DeploymentOptions().setConfig(config);
  }

  public static void closeContainers(
      KafkaContainer kafkaContainer,
      JdbcDatabaseContainer<?> jdbcDatabaseContainer,
      DebeziumContainer debeziumContainer,
      Network network) {
    kafkaContainer.close();
    jdbcDatabaseContainer.close();
    debeziumContainer.close();
    network.close();
  }

  @SuppressWarnings("UnstableApiUsage")
  public static String readResource(String name) throws IOException {
    return Resources.toString(Resources.getResource(name), Charsets.UTF_8);
  }

  public static String readResource(String name, VertxTestContext context) {
    String resource = null;
    try {
      resource = readResource(name);
    } catch (IOException e) {
      context.failNow(e);
    }
    return resource;
  }

  public static Stream<String> queryDirectories() throws IOException {
    return getResourceDirectories("queries");
  }

  public static Stream<String> subscriptionDirectories() throws IOException {
    return getResourceDirectories("subscriptions");
  }

  private static Stream<String> getResourceDirectories(String basePathName) throws IOException {
    int resourceRootNameCount = getResourceRoot().getNameCount() - 1;
    Path basePath = getBasePath(basePathName);
    Stream<Path> stream = Files.walk(basePath, 2);
    return stream
        .filter(
            path ->
                Files.isDirectory(path)
                    && !path.getParent().equals(basePath)
                    && !path.equals(basePath))
        .map(path -> path.subpath(resourceRootNameCount, path.getNameCount()))
        .map(Path::toString);
  }

  private static Path getResourceRoot() {
    return Paths.get(Resources.getResource("").getPath());
  }

  private static Path getBasePath(String dir) {
    return Paths.get(Resources.getResource(dir).getPath());
  }
}
