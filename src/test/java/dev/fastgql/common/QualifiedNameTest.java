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
  private QualifiedName qualifiedName;

  @Test
  public void shouldGenerateQualifiedName() {
    assertEquals("tableName/keyName", QualifiedName.generate("tableName", "keyName"));
  }

  @Test
  public void shouldConstructFromQualifiedName() {
    qualifiedName = new QualifiedName("tableName/keyName");
    assertEquals("tableName", qualifiedName.getTableName());
    assertEquals("keyName", qualifiedName.getKeyName());
    assertEquals("tableName/keyName", qualifiedName.getQualifiedName());
  }

  @Test
  public void shouldThrowIllegalArgumentWhenConstructFromQualifiedName() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QualifiedName("tableName"),
        "qualified name has to be in the format of \"table/key\""
    );
  }

  @Test
  public void shouldConstructFromTableNameKeyName() {
    qualifiedName = new QualifiedName("tableName", "keyName");
    assertEquals("tableName", qualifiedName.getTableName());
    assertEquals("keyName", qualifiedName.getKeyName());
    assertEquals("tableName/keyName", qualifiedName.getQualifiedName());
  }

  @Test
  public void shouldThrowIllegalArgumentWhenConstructFromTableNameKeyNameWithTableNameEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QualifiedName("", "keyName"),
        "qualified name: table or key cannot be empty"
    );
  }

  @Test
  public void shouldThrowIllegalArgumentWhenConstructFromTableNameKeyNameWithKeyNameEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new QualifiedName("tableName", ""),
        "qualified name: table or key cannot be empty"
    );
  }
}
