/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KeyDefinitionTest {

  KeyDefinition keyDefinition;

  @BeforeEach
  public void setUp() {
    keyDefinition =
        new KeyDefinition(
            new QualifiedName("tableName1", "keyName1"),
            KeyType.STRING,
            new QualifiedName("tableName2", "keyName2"),
            new HashSet<>(
                Arrays.asList(
                    new QualifiedName("tableName3", "keyName3"),
                    new QualifiedName("tableName4", "keyName4"))));
  }

  @Test
  public void merge_equal() {
    Set<QualifiedName> thisReferences = keyDefinition.getReferencedBy();
    keyDefinition.merge(keyDefinition);

    assertTrue(thisReferences.containsAll(keyDefinition.getReferencedBy()));
  }

  @Test
  public void merge_nullKey() {
    Exception exception = assertThrows(RuntimeException.class, () -> keyDefinition.merge(null));

    String expectedMessage = "cannot merge with null key";
    String actualMessage = exception.getMessage();

    assertTrue(actualMessage.contains(expectedMessage));
  }

  @Test
  public void merge_incompatibleKeys() {
    KeyDefinition otherKeyDefinition =
        new KeyDefinition(
            new QualifiedName("tableName1x", "keyName1x"),
            KeyType.STRING,
            new QualifiedName("tableName2x", "keyName2x"),
            new HashSet<>(Collections.singletonList(new QualifiedName("tableName6", "keyName6"))));
    assertThrows(
        RuntimeException.class,
        () -> keyDefinition.merge(otherKeyDefinition),
        "keys to be merged not compatible");
  }

  @Test
  public void merge_alreadyReferenced() {
    KeyDefinition otherKeyDefinition =
        new KeyDefinition(
            new QualifiedName("tableName1", "keyName1"),
            KeyType.STRING,
            new QualifiedName("tableName2x", "keyName2x"),
            new HashSet<>(Collections.singletonList(new QualifiedName("tableName6", "keyName6"))));
    assertThrows(
        RuntimeException.class,
        () -> keyDefinition.merge(otherKeyDefinition),
        "key is already referencing other key");
  }

  @Test
  public void merge_differentReferencedBy() {
    KeyDefinition otherKeyDefinition =
        new KeyDefinition(
            new QualifiedName("tableName1", "keyName1"),
            KeyType.STRING,
            new QualifiedName("tableName2", "keyName2"),
            new HashSet<>(Collections.singletonList(new QualifiedName("tableName6", "keyName6"))));

    // saving both sets of references in order to generate the expected set
    Set<QualifiedName> thisReferences = keyDefinition.getReferencedBy();
    Set<QualifiedName> otherReferences = otherKeyDefinition.getReferencedBy();
    thisReferences.addAll(otherReferences);

    // merging of KeyDefinitions
    keyDefinition.merge(otherKeyDefinition);

    assertTrue(keyDefinition.getReferencedBy().containsAll(thisReferences));
  }

  @Test
  public void merge_nullReferencedBy() {
    KeyDefinition otherKeyDefinition =
        new KeyDefinition(
            new QualifiedName("tableName1", "keyName1"),
            KeyType.STRING,
            new QualifiedName("tableName2", "keyName2"),
            null);

    // saving the set of 'this' references as the expected set
    Set<QualifiedName> thisReferences = keyDefinition.getReferencedBy();

    // merging of KeyDefinitions
    keyDefinition.merge(otherKeyDefinition);

    assertTrue(keyDefinition.getReferencedBy().containsAll(thisReferences));
  }
}
