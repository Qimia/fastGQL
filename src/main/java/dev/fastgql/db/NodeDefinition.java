/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import dev.fastgql.common.FieldType;
import dev.fastgql.common.QualifiedName;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Definition of a single field in a table, intended to be use as
 * part of {@link DatabaseSchema}.
 *
 * @author Kamil Bobrowski
 */
public class NodeDefinition {
  private final QualifiedName qualifiedName;
  private final FieldType fieldType;
  private QualifiedName referencing;
  private Set<QualifiedName> referencedBy;

  /**
   * Define table field.
   *
   * @param qualifiedName qualified name which specifies which field of which table
   *                      is being defined
   * @param fieldType type of field
   * @param referencing which field is being referenced by this field
   * @param referencedBySet set of fields which are referencing this field
   */
  public NodeDefinition(
      QualifiedName qualifiedName,
      FieldType fieldType,
      QualifiedName referencing,
      Set<QualifiedName> referencedBySet) {
    Objects.requireNonNull(qualifiedName);
    Objects.requireNonNull(fieldType);
    this.qualifiedName = qualifiedName;
    this.fieldType = fieldType;
    this.referencing = referencing;
    this.referencedBy = Objects.requireNonNullElseGet(referencedBySet, HashSet::new);
  }

  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  public FieldType getFieldType() {
    return fieldType;
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
    NodeDefinition otherNode = (NodeDefinition) o;
    return Objects.equals(qualifiedName, otherNode.qualifiedName)
        && Objects.equals(fieldType, otherNode.fieldType)
        && Objects.equals(referencing, otherNode.referencing)
        && Objects.equals(referencedBy, otherNode.referencedBy);
  }

  /**
   * Merge information from other field to this field. If two nodes are compatible
   * (the same qualified name, field type and another field being referenced)
   * it will add all fields which are referencing another field to this field.
   *
   * @param otherNode - other field to be merged with this field
   */
  public void merge(NodeDefinition otherNode) {
    if (this == otherNode) {
      return;
    }
    if (otherNode == null) {
      throw new RuntimeException("cannot merge with null node");
    }
    if (!(Objects.equals(qualifiedName, otherNode.qualifiedName)
        && Objects.equals(fieldType, otherNode.fieldType))) {
      throw new RuntimeException("nodes to be merged not compatible");
    }
    if (referencing != null
        && otherNode.referencing != null
        && !referencing.equals(otherNode.referencing)) {
      throw new RuntimeException("node is already referencing other node");
    }
    if (otherNode.referencing != null) {
      this.referencing = otherNode.referencing;
    }
    if (otherNode.referencedBy == null) {
      return;
    }
    for (QualifiedName qualifiedName : otherNode.referencedBy) {
      addReferredBy(qualifiedName);
    }
  }

  @Override
  public String toString() {
    return "NodeDefinition{"
        + "qualifiedName='"
        + qualifiedName
        + '\''
        + ", nodeType="
        + fieldType
        + ", referencing='"
        + referencing
        + '\''
        + ", referredBy="
        + referencedBy
        + '}';
  }
}
