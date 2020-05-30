package ai.qimia.fastgql.schema;

import java.util.*;

public class NodeDefinition {
  private final QualifiedName qualifiedName;
  private final FieldType fieldType;
  private QualifiedName referencing;
  private Set<QualifiedName> referredBy;

  public NodeDefinition(QualifiedName qualifiedName, FieldType fieldType, QualifiedName referencing, Set<QualifiedName> referredBy) {
    Objects.requireNonNull(qualifiedName);
    Objects.requireNonNull(fieldType);
    this.qualifiedName = qualifiedName;
    this.fieldType = fieldType;
    this.referencing = referencing;
    this.referredBy = Objects.requireNonNullElseGet(referredBy, HashSet::new);
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

  public Set<QualifiedName> getReferredBy() {
    return referredBy;
  }

  public void addReferredBy(QualifiedName name) {
    this.referredBy.add(name);
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
    return Objects.equals(qualifiedName, otherNode.qualifiedName) &&
      Objects.equals(fieldType, otherNode.fieldType) &&
      Objects.equals(referencing, otherNode.referencing) &&
      Objects.equals(referredBy, otherNode.referredBy);
  }

  public void merge(NodeDefinition otherNode) {
    if (this == otherNode) {
      return;
    }
    if (otherNode == null) {
      throw new RuntimeException("cannot merge with null node");
    }
    if (!(Objects.equals(qualifiedName, otherNode.qualifiedName) &&
      Objects.equals(fieldType, otherNode.fieldType))) {
      throw new RuntimeException("nodes to be merged not compatible");
    }
    if (referencing != null && otherNode.referencing != null && !referencing.equals(otherNode.referencing)) {
      throw new RuntimeException("node is already referencing other node");
    }
    if (otherNode.referencing != null) {
      this.referencing = otherNode.referencing;
    }
    if (otherNode.referredBy == null) {
      return;
    }
    for (QualifiedName qualifiedName: otherNode.referredBy) {
      addReferredBy(qualifiedName);
    }
  }

  @Override
  public String toString() {
    return "NodeDefinition{" +
      "qualifiedName='" + qualifiedName + '\'' +
      ", nodeType=" + fieldType +
      ", referencing='" + referencing + '\'' +
      ", referredBy=" + referredBy +
      '}';
  }

  static class Builder {
    public Builder() {}


  }
}
