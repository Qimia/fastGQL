/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package ai.qimia.fastgql;

import lombok.Data;

@Data
public class DatasourceConfig {

  private String host = "localhost";
  private int port = 5432;
  private String db = "quarkus_test";
  private String username = "quarkus_test";
  private String password = "quarkus_test";
}
