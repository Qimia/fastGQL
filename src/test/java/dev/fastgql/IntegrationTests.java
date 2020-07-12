package dev.fastgql;

import dev.fastgql.integration.*;
import dev.fastgql.integration.mysql.WithMySQL57;
import dev.fastgql.integration.mysql.WithMySQL8;
import dev.fastgql.integration.postgres.WithPostgres10;
import dev.fastgql.integration.postgres.WithPostgres11;
import dev.fastgql.integration.postgres.WithPostgres12;
import dev.fastgql.integration.postgres.WithPostgres96;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class IntegrationTests {

  @Nested
  @DisplayName("PostgreSQL 9.6 Tests")
  class Postgres96 {
    @Nested
    @DisplayName("PostgreSQL 9.6 Query Tests")
    class PostgreskltgrQuery extends ContainerEnvWithDatabase implements WithPostgres96, QueryTests {}

    @Nested
    @DisplayName("PostgreSQL 9.6 Subscription Tests with external Debezium")
    class PostgresSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithPostgres96, WithPostgresConnector, SubscriptionTests {}

    @Nested
    @DisplayName("PostgreSQL 9.6 Subscription Tests with embedded Debezium")
    class PostgresSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithPostgres96, WithEmbeddedDebezium, SubscriptionTests {}
  }

  @Nested
  @DisplayName("PostgreSQL 10 Tests")
  class Postgres10 {
    @Nested
    @DisplayName("PostgreSQL 10 Query Tests")
    class PostgresQuery extends ContainerEnvWithDatabase implements WithPostgres10, QueryTests {}

    @Nested
    @DisplayName("PostgreSQL 10 Subscription Tests with external Debezium")
    class PostgresSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithPostgres10, WithPostgresConnector, SubscriptionTests {}

    @Nested
    @DisplayName("PostgreSQL 10 Subscription Tests with embedded Debezium")
    class PostgresSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithPostgres10, WithEmbeddedDebezium, SubscriptionTests {}
  }

  @Nested
  @DisplayName("PostgreSQL 11 Tests")
  class Postgres11 {
    @Nested
    @DisplayName("PostgreSQL 11 Query Tests")
    class PostgresQuery extends ContainerEnvWithDatabase implements WithPostgres11, QueryTests {}

    @Nested
    @DisplayName("PostgreSQL 11 Subscription Tests with external Debezium")
    class PostgresSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithPostgres11, WithPostgresConnector, SubscriptionTests {}

    @Nested
    @DisplayName("PostgreSQL 11 Subscription Tests with embedded Debezium")
    class PostgresSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithPostgres11, WithEmbeddedDebezium, SubscriptionTests {}
  }

  @Nested
  @DisplayName("PostgreSQL 12 Tests")
  class Postgres12 {
    @Nested
    @DisplayName("PostgreSQL 12 Query Tests")
    class PostgresQuery extends ContainerEnvWithDatabase implements WithPostgres12, QueryTests {}

    @Nested
    @DisplayName("PostgreSQL 12 Subscription Tests with external Debezium")
    class PostgresSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithPostgres12, WithPostgresConnector, SubscriptionTests {}

    @Nested
    @DisplayName("PostgreSQL 12 Subscription Tests with embedded Debezium")
    class PostgresSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithPostgres12, WithEmbeddedDebezium, SubscriptionTests {}
  }

  @Nested
  @DisplayName("MySQL 5.7 Tests")
  class MySQL57 {
    @Nested
    @DisplayName("MySQL 5.7 Query Tests")
    class MySQLQuery extends ContainerEnvWithDatabase implements WithMySQL57, QueryTests {}

    @Nested
    @DisplayName("MySQL 5.7 Subscription Tests with external Debezium")
    class MySQLSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithMySQL57, WithMySQLConnector, SubscriptionTests {}

    @Nested
    @DisplayName("MySQL 5.7 Subscription Tests with embedded Debezium")
    class MySQLSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithMySQL57, WithEmbeddedDebezium, SubscriptionTests {}
  }

  @Nested
  @DisplayName("MySQL 8 Tests")
  class MySQL {
    @Nested
    @DisplayName("MySQL 8 Query Tests")
    class MySQLQuery extends ContainerEnvWithDatabase implements WithMySQL8, QueryTests {}

    @Nested
    @DisplayName("MySQL 8 Subscription Tests with external Debezium")
    class MySQLSubscriptionExternalDebezium extends ContainerEnvWithDatabaseAndDebezium
        implements WithMySQL8, WithMySQLConnector, SubscriptionTests {}

    @Nested
    @DisplayName("MySQL 8 Subscription Tests with embedded Debezium")
    class MySQLSubscriptionEmbeddedDebezium extends ContainerEnvWithDatabase
        implements WithMySQL8, WithEmbeddedDebezium, SubscriptionTests {}
  }
}
