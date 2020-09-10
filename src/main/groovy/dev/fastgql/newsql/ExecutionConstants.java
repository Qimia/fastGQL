package dev.fastgql.newsql;

import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.graphql.GraphQLDatabaseSchema;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.Map;

public class ExecutionConstants {
  private final Transaction transaction;
  private final GraphQLDatabaseSchema graphQLDatabaseSchema;
  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;

  public ExecutionConstants(
      Transaction transaction,
      GraphQLDatabaseSchema graphQLDatabaseSchema,
      RoleSpec roleSpec,
      Map<String, Object> jwtParams) {
    this.transaction = transaction;
    this.graphQLDatabaseSchema = graphQLDatabaseSchema;
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public GraphQLDatabaseSchema getGraphQLDatabaseSchema() {
    return graphQLDatabaseSchema;
  }

  public RoleSpec getRoleSpec() {
    return roleSpec;
  }

  public Map<String, Object> getJwtParams() {
    return jwtParams;
  }
}
