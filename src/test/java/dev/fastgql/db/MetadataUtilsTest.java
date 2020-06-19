/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import dev.fastgql.integration.DBTestUtils;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MetadataUtilsTest {

  @Container
  PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("debezium/postgres:11");

  DatasourceConfig datasourceConfig;

  @BeforeEach
  public void setUp() throws IOException, SQLException {
    datasourceConfig =
        DatasourceConfig.createDatasourceConfig(
            postgreSQLContainer.getJdbcUrl(),
            postgreSQLContainer.getUsername(),
            postgreSQLContainer.getPassword(),
            "public");
    DBTestUtils.executeSQLQueryFromResource("db/metadataUtilsTest.sql", postgreSQLContainer);
  }

  @Test
  public void shouldCreateDatabaseSchema() throws SQLException {
    Connection connection = datasourceConfig.getConnection();
    DatabaseSchema databaseSchema = MetadataUtils.createDatabaseSchema(connection);
    KeyDefinition expectedAddressesIdKeyDefinition =
        new KeyDefinition(
            new QualifiedName("addresses/id"),
            KeyType.INT,
            null,
            Set.of(new QualifiedName("customers/address")));
    KeyDefinition expectedCustomersAddressKeyDefinition =
        new KeyDefinition(
            new QualifiedName("customers/address"),
            KeyType.INT,
            new QualifiedName("addresses/id"),
            null);
    KeyDefinition expectedCustomersIdKeyDefinition =
        new KeyDefinition(new QualifiedName("customers/id"), KeyType.INT, null, null);

    assertTrue(databaseSchema.getTableNames().containsAll(List.of("customers", "addresses")));
    assertEquals(2, databaseSchema.getTableNames().size());
    assertEquals(
        expectedAddressesIdKeyDefinition, databaseSchema.getGraph().get("addresses").get("id"));
    assertEquals(
        expectedCustomersAddressKeyDefinition,
        databaseSchema.getGraph().get("customers").get("address"));
    assertEquals(
        expectedCustomersIdKeyDefinition, databaseSchema.getGraph().get("customers").get("id"));
    connection.close();
  }
}
