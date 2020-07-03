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
    class PostgresQuery extends ContainerEnvWithPostgres implements QueryTests {}

    @Nested
    @DisplayName("PostgreSQL Subscription Tests with external Debezium")
    class PostgresSubscriptionExternalDebezium extends ContainerEnvWithPostgresDebezium implements SubscriptionTests {}

    @Nested
    @DisplayName("PostgreSQL Subscription Tests with embedded Debezium")
    class PostgresSubscriptionEmbeddedDebezium extends ContainerEnvWithPostgres implements SubscriptionTests {}
  }

  @Nested
  @DisplayName("MySQL Tests")
  class MySQL {
    @Nested
    @DisplayName("MySQL Query Tests")
    class MySQLQuery extends ContainerEnvWithMySQL implements QueryTests {}

    @Nested
    @DisplayName("MySQL Subscription Tests")
    class MySQLSubscription extends ContainerEnvWithMySQLDebezium implements SubscriptionTests {}
  }
}
