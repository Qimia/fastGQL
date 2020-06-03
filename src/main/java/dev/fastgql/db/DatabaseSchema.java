package dev.fastgql.db;

import dev.fastgql.common.FieldType;
import dev.fastgql.common.QualifiedName;

import java.util.*;

public class DatabaseSchema {
  private final Map<String, Map<String, NodeDefinition>> graph;

  public static DatabaseSchema.Builder newSchema() {
    return new DatabaseSchema.Builder();
  }

  public Map<String, Map<String, NodeDefinition>> getGraph() {
    return graph;
  }

  public Set<String> getTableNames() {
    return graph.keySet();
  }

  @Override
  public String toString() {
    return "DatabaseSchema{" +
      "graph=" + graph +
      '}';
  }

  public static class Builder {
    private Map<String, Map<String, NodeDefinition>> graph = new HashMap<>();

    public Builder() {}

    public DatabaseSchema build() {
      return new DatabaseSchema(graph);
    }

    public Builder row(String table, String field, FieldType type, String referencingTable, String referencingField) {
      Objects.requireNonNull(table);
      Objects.requireNonNull(field);
      Objects.requireNonNull(type);
      Objects.requireNonNull(referencingTable);
      Objects.requireNonNull(referencingField);
      addNode(new NodeDefinition(new QualifiedName(table, field), type, new QualifiedName(referencingTable, referencingField), null));
      return this;
    }

    public Builder row(String qualifiedName, FieldType type, String qualifiedReferencingName) {
      Objects.requireNonNull(qualifiedName);
      Objects.requireNonNull(type);
      Objects.requireNonNull(qualifiedReferencingName);
      addNode(new NodeDefinition(new QualifiedName(qualifiedName), type, new QualifiedName(qualifiedReferencingName), null));
      return this;
    }

    public Builder row(String table, String field, FieldType type) {
      Objects.requireNonNull(table);
      Objects.requireNonNull(field);
      Objects.requireNonNull(type);
      addNode(new NodeDefinition(new QualifiedName(table, field), type, null, null));
      return this;
    }

    public Builder row(String qualifiedName, FieldType type) {
      Objects.requireNonNull(qualifiedName);
      Objects.requireNonNull(type);
      addNode(new NodeDefinition(new QualifiedName(qualifiedName), type, null, null));
      return this;
    }

    private NodeDefinition nodeAt(QualifiedName qualifiedName) {
      String parent = qualifiedName.getParent();
      String name = qualifiedName.getName();
      if (graph.containsKey(parent) && graph.get(parent).containsKey(name)) {
        return graph.get(parent).get(name);
      } else {
        return null;
      }
    }

    private void mergeNodeAt(QualifiedName qualifiedName, NodeDefinition node) {
      Objects.requireNonNull(nodeAt(qualifiedName)).merge(node);
    }

    private void addNodeAt(QualifiedName qualifiedName, NodeDefinition node) {
      String parent = qualifiedName.getParent();
      String name = qualifiedName.getName();
      if (!graph.containsKey(parent)) {
        graph.put(parent, new HashMap<>());
      }
      graph.get(parent).put(name, node);
    }

    private void addNode(NodeDefinition newNode) {
      QualifiedName qualifiedName = newNode.getQualifiedName();
      if (nodeAt(qualifiedName) != null) {
        mergeNodeAt(qualifiedName, newNode);
      } else {
        addNodeAt(qualifiedName, newNode);
      }
      QualifiedName referencing = newNode.getReferencing();
      if (referencing != null) {
        addNode(new NodeDefinition(referencing, newNode.getFieldType(), null, Set.of(qualifiedName)));
      }
    }
  }

  private DatabaseSchema(Map<String, Map<String, NodeDefinition>> graph) {
    this.graph = graph;
  }
}
