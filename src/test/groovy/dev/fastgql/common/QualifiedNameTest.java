/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class QualifiedNameTest {

  @Test
  public void generate() {
    assertEquals("tableName/keyName", QualifiedName.generate("tableName", "keyName"));
  }

  @Test
  public void constructor_oneArg() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    assertEquals("tableName", qualifiedName.getTableName());
    assertEquals("keyName", qualifiedName.getKeyName());
    assertEquals("tableName/keyName", qualifiedName.getQualifiedName());
  }

  @Test
  public void constructor_invalidOneArg() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QualifiedName("tableName"),
        "qualified name has to be in the format of \"table/key\"");
  }

  @Test
  public void constructor_twoArg() {
    QualifiedName qualifiedName = new QualifiedName("tableName", "keyName");
    assertEquals("tableName", qualifiedName.getTableName());
    assertEquals("keyName", qualifiedName.getKeyName());
    assertEquals("tableName/keyName", qualifiedName.getQualifiedName());
  }

  @Test
  public void constructor_twoArgEmptyFirstArg() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QualifiedName("", "keyName"),
        "qualified name: table or key cannot be empty");
  }

  @Test
  public void constructor_twoArgEmptySecondArg() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QualifiedName("tableName", ""),
        "qualified name: table or key cannot be empty");
  }
}
