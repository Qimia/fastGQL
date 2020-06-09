package dev.fastgql;

import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class TestcontainersSqlDebug {
  public static void main(String[] args) throws SQLException {
    MySQLContainer<?> mysqlContainer = new MySQLContainer<>("fastgql/mysql-testcontainers:latest");

    mysqlContainer.start();

    try (Connection connection = DriverManager.getConnection(mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(), mysqlContainer.getPassword());
         Statement statement = connection.createStatement()) {
      statement.execute("SELECT 1");
    }

  }
}
