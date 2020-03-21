package ai.qimia.vertxtest;

import lombok.Data;

@Data
public class DatasourceConfig {
  private String host;
  private int port;
  private String db;
  private String username;
  private String password;
}
