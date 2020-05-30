package ai.qimia.fastgql.schema;

import org.antlr.v4.runtime.misc.Pair;

import java.util.*;

public class NodeDefinition {
  private final QualifiedName qualifiedName;
  private final NodeType nodeType;
  private QualifiedName referencing;
  private Set<QualifiedName> referredBy;

  public NodeDefinition(QualifiedName qualifiedName, NodeType nodeType, QualifiedName referencing, Set<QualifiedName> referredBy) {
    Objects.requireNonNull(qualifiedName);
    Objects.requireNonNull(nodeType);
    this.qualifiedName = qualifiedName;
    this.nodeType = nodeType;
    this.referencing = referencing;
    this.referredBy = Objects.requireNonNullElseGet(referredBy, HashSet::new);
  }

  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  public NodeType getNodeType() {
    return nodeType;
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
      Objects.equals(nodeType, otherNode.nodeType) &&
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
      Objects.equals(nodeType, otherNode.nodeType))) {
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
      ", nodeType=" + nodeType +
      ", referencing='" + referencing + '\'' +
      ", referredBy=" + referredBy +
      '}';
  }
}
