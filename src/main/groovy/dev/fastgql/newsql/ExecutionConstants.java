package dev.fastgql.newsql;

import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.graphql.GraphQLDatabaseSchema;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ExecutionConstants {
  private final QueryExecutor queryExecutor;
  private final GraphQLDatabaseSchema graphQLDatabaseSchema;
  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;
  private final Function<Set<TableAlias>, String> tableListLockQueryFunction;
  private final String unlockQuery;

  public ExecutionConstants(
      QueryExecutor queryExecutor,
      GraphQLDatabaseSchema graphQLDatabaseSchema,
      RoleSpec roleSpec,
      Map<String, Object> jwtParams,
      Function<Set<TableAlias>, String> tableListLockQueryFunction,
      String unlockQuery
      ) {
    this.queryExecutor = queryExecutor;
    this.graphQLDatabaseSchema = graphQLDatabaseSchema;
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
    this.tableListLockQueryFunction = tableListLockQueryFunction;
    this.unlockQuery = unlockQuery;
  }

  public QueryExecutor getQueryExecutor() {
    return queryExecutor;
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

  public String getUnlockQuery() {
    return unlockQuery;
  }

  public Function<Set<TableAlias>, String> getTableListLockQueryFunction() {
    return tableListLockQueryFunction;
  }
}
