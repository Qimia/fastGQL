package dev.fastgql;

import dev.fastgql.integration.ContainerEnvWithDatabase;
import dev.fastgql.integration.ContainerEnvWithDatabaseAndDebezium;
import dev.fastgql.integration.MutationTests;
import dev.fastgql.integration.QueryTests;
import dev.fastgql.integration.SubscriptionTests;
import dev.fastgql.integration.WithEmbeddedDebezium;
import dev.fastgql.integration.WithMySQL;
import dev.fastgql.integration.WithMySQLConnector;
import dev.fastgql.integration.WithPostgres;
import dev.fastgql.integration.WithPostgresConnector;
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
    @DisplayName("PostgreSQL Mutation Tests")
    class PostgresMutation extends ContainerEnvWithDatabase
        implements WithPostgres, MutationTests {}

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
    @DisplayName("MySQL Mutation Tests")
    class MySQLMutation extends ContainerEnvWithDatabase implements WithMySQL, MutationTests {}

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
