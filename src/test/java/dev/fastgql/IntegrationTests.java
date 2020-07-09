package dev.fastgql;

import dev.fastgql.integration.*;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class IntegrationTests {

  @Nested
  @DisplayName("PostgreSQL Tests")
  class Postgres {
    @Nested
    @DisplayName("PostgreSQL Query Tests")
    class PostgresQuery extends ContainerEnvWithDatabase implements WithPostgres, QueryTests {}

    @Nested
    @DisplayName("PostgreSQL Subscription Tests with external Debezium")
    class PostgresSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithPostgres, WithPostgresConnector, SubscriptionTests {}

    @Nested
    @DisplayName("PostgreSQL Subscription Tests with embedded Debezium")
    class PostgresSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithPostgres, WithEmbeddedDebezium, SubscriptionTests {}
  }

  @Nested
  @DisplayName("MySQL Tests")
  class MySQL {
    @Nested
    @DisplayName("MySQL Query Tests")
    class MySQLQuery extends ContainerEnvWithDatabase implements WithMySQL, QueryTests {}

    @Nested
    @DisplayName("MySQL Subscription Tests with external Debezium")
    class MySQLSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithMySQL, WithMySQLConnector, SubscriptionTests {}

    @Nested
    @DisplayName("MySQL Subscription Tests with embedded Debezium")
    class MySQLSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithMySQL, WithEmbeddedDebezium, SubscriptionTests {}
  }
}
