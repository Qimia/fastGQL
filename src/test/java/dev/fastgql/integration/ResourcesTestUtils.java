/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Test utils for handling resources.
 *
 * @author Mingyi Zhang
 */
public class ResourcesTestUtils {

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

  public static Stream<String> mutationDirectories() throws IOException {
    return getResourceDirectories("mutations");
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

  @SuppressWarnings("UnstableApiUsage")
  private static Path getResourceRoot() {
    return Paths.get(Resources.getResource("").getPath());
  }

  @SuppressWarnings("UnstableApiUsage")
  private static Path getBasePath(String dir) {
    return Paths.get(Resources.getResource(dir).getPath());
  }
}
