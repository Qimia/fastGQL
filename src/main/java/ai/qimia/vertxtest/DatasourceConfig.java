package ai.qimia.vertxtest;

import lombok.Data;

@Data
public class DatasourceConfig {
  private String host = "localhost";
  private int port = 5432;
  private String db = "quarkus_test";
  private String username = "quarkus_test";
  private String password = "quarkus_test";
}
