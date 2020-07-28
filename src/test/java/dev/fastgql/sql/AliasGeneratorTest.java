/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class AliasGeneratorTest {

  @Test
  public void getAlias() {
    AliasGenerator aliasGenerator = new AliasGenerator();
    assertEquals("a0", aliasGenerator.getAlias());
    assertEquals("a1", aliasGenerator.getAlias());
    assertEquals("a2", aliasGenerator.getAlias());
  }
}
