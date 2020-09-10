package dev.fastgql.newsql;

import dev.fastgql.dsl.PermissionsSpec;
import dev.fastgql.graphql.GraphQLDatabaseSchema;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.Map;

public class SQLExecutionConstants {
  private final Transaction transaction;
  private final GraphQLDatabaseSchema graphQLDatabaseSchema;
  private final PermissionsSpec permissionsSpec;
  private final String role;
  private final Map<String, Object> jwtParams;

  public SQLExecutionConstants(
      Transaction transaction,
      GraphQLDatabaseSchema graphQLDatabaseSchema,
      PermissionsSpec permissionsSpec,
      String role,
      Map<String, Object> jwtParams) {
    this.transaction = transaction;
    this.graphQLDatabaseSchema = graphQLDatabaseSchema;
    this.permissionsSpec = permissionsSpec;
    this.role = role;
    this.jwtParams = jwtParams;
  }

  public Transaction getTransaction() {
    return transaction;
  }

  public GraphQLDatabaseSchema getGraphQLDatabaseSchema() {
    return graphQLDatabaseSchema;
  }

  public PermissionsSpec getPermissionsSpec() {
    return permissionsSpec;
  }

  public String getRole() {
    return role;
  }

  public Map<String, Object> getJwtParams() {
    return jwtParams;
  }
}
