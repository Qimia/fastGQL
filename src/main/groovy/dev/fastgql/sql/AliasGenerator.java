/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

public class AliasGenerator {
  private long counter = 0;

  public String getAlias() {
    return String.format("a%d", counter++);
  }
}
