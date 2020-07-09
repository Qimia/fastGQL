package dev.fastgql.integration;

import org.testcontainers.containers.JdbcDatabaseContainer;

public abstract class ContainerEnvWithDatabase implements WithFastGQL {
  private final JdbcDatabaseContainer<?> jdbcDatabaseContainer = createJdbcDatabaseContainer();
  private String deploymentID;

  @Override
  public JdbcDatabaseContainer<?> getJdbcDatabaseContainer() {
    return jdbcDatabaseContainer;
  }

  @Override
  public String getDeploymentID() {
    return deploymentID;
  }

  @Override
  public void setDeploymentID(String deploymentID) {
    this.deploymentID = deploymentID;
  }
}
