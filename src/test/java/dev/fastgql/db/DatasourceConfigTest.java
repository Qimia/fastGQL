package dev.fastgql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class DatasourceConfigTest {

  @Test
  void shouldParsePostgres() {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createDatasourceConfig(
            "jdbc:postgresql://localhost:5432/db", "user", "pswd");
    assertEquals("localhost", datasourceConfig.getHost());
    assertEquals(5432, datasourceConfig.getPort());
    assertEquals("db", datasourceConfig.getDb());
  }

  @Test
  void shouldParseWithOptionalArguments() {
    DatasourceConfig datasourceConfig =
      DatasourceConfig.createDatasourceConfig(
        "jdbc:postgresql://localhost:5432/db?optional=true", "user", "pswd");
    assertEquals("localhost", datasourceConfig.getHost());
    assertEquals(5432, datasourceConfig.getPort());
    assertEquals("db", datasourceConfig.getDb());
  }

  @Test
  void shouldParseMysql() {
    DatasourceConfig datasourceConfig =
        DatasourceConfig.createDatasourceConfig("jdbc:mysql://localhost:5432/db", "user", "pswd");
    assertEquals("localhost", datasourceConfig.getHost());
    assertEquals(5432, datasourceConfig.getPort());
    assertEquals("db", datasourceConfig.getDb());
  }

  @Test
  void shouldThrowPortTooShort() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:postgresql://localhost:543/db", "user", "pswd"),
        "JDBC url not valid");
  }

  @Test
  void shouldThrowPortTooLong() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:postgresql://localhost:543210/db", "user", "pswd"),
        "JDBC url not valid");
  }

  @Test
  void shouldThrowNoDb() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:postgresql://localhost:5432/", "user", "pswd"),
        "JDBC url not valid");
  }

  @Test
  void shouldThrowUnsupportedDb() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            DatasourceConfig.createDatasourceConfig(
                "jdbc:anothersql://localhost:5432/db", "user", "pswd"),
        "Unsupported DB type");
  }
}
