/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import dev.fastgql.integration.DBTestUtils;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.VertxModule;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Pool;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(VertxExtension.class)
public class MetadataUtilsTest {

  @Container
  PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("debezium/postgres:11");

  JsonObject config;
  Pool pool;

  DatasourceConfig datasourceConfig;

  @BeforeEach
  public void setUp(Vertx vertx, VertxTestContext context) {
    config =
        new JsonObject(
            Map.of(
                "datasource",
                Map.of(
                    "jdbcUrl",
                    postgreSQLContainer.getJdbcUrl(),
                    "username",
                    postgreSQLContainer.getUsername(),
                    "password",
                    postgreSQLContainer.getPassword(),
                    "schema",
                    "public")));

    Injector injector = Guice.createInjector(new VertxModule(vertx, config), new DatabaseModule());
    pool = injector.getInstance(Pool.class);
    datasourceConfig = injector.getInstance(DatasourceConfig.class);

    DBTestUtils.executeSQLQuery("db/metadataUtilsTest.sql", pool)
        .subscribe(rows -> context.completeNow(), context::failNow);
  }

  @Test
  public void createDatabaseSchema(Vertx vertx, VertxTestContext context) {
    Completable.fromCallable(
            () -> {
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

              context.verify(
                  () -> {
                    assertTrue(
                        databaseSchema
                            .getTableNames()
                            .containsAll(List.of("customers", "addresses")));
                    assertEquals(2, databaseSchema.getTableNames().size());
                    assertEquals(
                        expectedAddressesIdKeyDefinition,
                        databaseSchema.getGraph().get("addresses").get("id"));
                    assertEquals(
                        expectedCustomersAddressKeyDefinition,
                        databaseSchema.getGraph().get("customers").get("address"));
                    assertEquals(
                        expectedCustomersIdKeyDefinition,
                        databaseSchema.getGraph().get("customers").get("id"));
                  });
              connection.close();
              return null;
            })
        .subscribeOn(Schedulers.io())
        .subscribe(context::completeNow, context::failNow);
  }
}
