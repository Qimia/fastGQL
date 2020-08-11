package dev.fastgql;

import dev.fastgql.integration.ContainerEnvWithDatabase;
import dev.fastgql.integration.WithMySQL;
import dev.fastgql.integration.WithPostgres;
import dev.fastgql.transaction.TransactionTest;
import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TransactionTests {
  @Nested
  class PostgresTransactionTest extends ContainerEnvWithDatabase
      implements WithPostgres, TransactionTest {}

  @Nested
  class MySQLTransactionTest extends ContainerEnvWithDatabase
      implements WithMySQL, TransactionTest {}
}
