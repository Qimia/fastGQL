package dev.fastgql.integration;

import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class IntegrationTests {
  @Nested
  @DisplayName("PostgreSQL Query Tests")
  class PostgresQueryIT extends ContainerEnvWithPostgres implements QueryTests {}

  @Nested
  @DisplayName("PostgreSQL Subscription Tests")
  class PostgresSubscriptionIT extends ContainerEnvWithPostgres implements SubscriptionTests {}

  @Nested
  @DisplayName("MySQL Query Tests")
  class MySQLQueryIT extends ContainerEnvWithMySQL implements QueryTests {}

  @Nested
  @DisplayName("MySQL Subscription Tests")
  class MySQLSubscriptionIT extends ContainerEnvWithMySQL implements SubscriptionTests {}
}
