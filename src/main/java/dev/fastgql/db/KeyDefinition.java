/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Definition of a single key in a table, intended to be use as part of {@link DatabaseSchema}.
 *
 * @author Kamil Bobrowski
 */
public class KeyDefinition {
  private final QualifiedName qualifiedName;
  private final KeyType keyType;
  private QualifiedName referencing;
  private Set<QualifiedName> referencedBy;

  /**
   * Define table key.
   *
   * @param qualifiedName qualified name which specifies which key of which table is being defined
   * @param keyType type of the key
   * @param referencing which key is being referenced by this key
   * @param referencedBySet set of keys which are referencing this key
   */
  public KeyDefinition(
      QualifiedName qualifiedName,
      KeyType keyType,
      QualifiedName referencing,
      Set<QualifiedName> referencedBySet) {
    Objects.requireNonNull(qualifiedName);
    Objects.requireNonNull(keyType);
    this.qualifiedName = qualifiedName;
    this.keyType = keyType;
    this.referencing = referencing;
    this.referencedBy = Objects.requireNonNullElseGet(referencedBySet, HashSet::new);
  }

  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  public KeyType getKeyType() {
    return keyType;
  }

  public QualifiedName getReferencing() {
    return referencing;
  }

  public Set<QualifiedName> getReferencedBy() {
    return referencedBy;
  }

  public void addReferredBy(QualifiedName name) {
    this.referencedBy.add(name);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KeyDefinition otherNode = (KeyDefinition) o;
    return Objects.equals(qualifiedName, otherNode.qualifiedName)
        && Objects.equals(keyType, otherNode.keyType)
        && Objects.equals(referencing, otherNode.referencing)
        && Objects.equals(referencedBy, otherNode.referencedBy);
  }

  /**
   * Merge information from other key to this key. If two keys are compatible (the same qualified
   * name, key type and another key being referenced) it will add all keys which are referencing
   * another key to this key.
   *
   * @param otherKeyDefinition - other key to be merged with this key
   */
  public void merge(KeyDefinition otherKeyDefinition) {
    if (this == otherKeyDefinition) {
      return;
    }
    if (otherKeyDefinition == null) {
      throw new RuntimeException("cannot merge with null key");
    }
    if (!(Objects.equals(qualifiedName, otherKeyDefinition.qualifiedName)
        && Objects.equals(keyType, otherKeyDefinition.keyType))) {
      throw new RuntimeException("keys to be merged not compatible");
    }
    if (referencing != null
        && otherKeyDefinition.referencing != null
        && !referencing.equals(otherKeyDefinition.referencing)) {
      throw new RuntimeException("key is already referencing other key");
    }
    if (otherKeyDefinition.referencing != null) {
      this.referencing = otherKeyDefinition.referencing;
    }
    if (otherKeyDefinition.referencedBy == null) {
      return;
    }
    for (QualifiedName qualifiedName : otherKeyDefinition.referencedBy) {
      addReferredBy(qualifiedName);
    }
  }

  @Override
  public String toString() {
    return "KeyDefinition{"
        + "qualifiedName='"
        + qualifiedName
        + '\''
        + ", keyType="
        + keyType
        + ", referencing='"
        + referencing
        + '\''
        + ", referredBy="
        + referencedBy
        + '}';
  }
}
