package dev.fastgql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DatasourceConfigTest {

  @Test
  void create_postgres() {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createDatasourceConfig(
            "jdbc:postgresql://localhost:5432/db", "user", "pswd", "public");
    assertEquals("localhost", datasourceConfig.getHost());
    assertEquals(5432, datasourceConfig.getPort());
    assertEquals("db", datasourceConfig.getDb());
  }

  @Test
  void create_postgresWithOptionalArgs() {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createDatasourceConfig(
            "jdbc:postgresql://localhost:5432/db?optional=true", "user", "pswd", "public");
    assertEquals("localhost", datasourceConfig.getHost());
    assertEquals(5432, datasourceConfig.getPort());
    assertEquals("db", datasourceConfig.getDb());
  }

  @Test
  void create_mysql() {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createDatasourceConfig("jdbc:mysql://localhost:5432/db", "user", "pswd");
    assertEquals("localhost", datasourceConfig.getHost());
    assertEquals(5432, datasourceConfig.getPort());
    assertEquals("db", datasourceConfig.getDb());
    assertEquals("db", datasourceConfig.getSchema());
  }

  @Test
  void create_tooShortUrl() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:postgresql://localhost:543/db", "user", "pswd"),
        "JDBC url not valid");
  }

  @Test
  void create_tooLongUrl() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:postgresql://localhost:543210/db", "user", "pswd"),
        "JDBC url not valid");
  }

  @Test
  void create_noDb() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:postgresql://localhost:5432/", "user", "pswd"),
        "JDBC url not valid");
  }

  @Test
  void create_unsupportedDb() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:anothersql://localhost:5432/db", "user", "pswd"),
        "Unsupported DB type");
  }
}
