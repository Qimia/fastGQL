/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import dev.fastgql.common.FieldType;
import dev.fastgql.common.QualifiedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data structure defining database schema, including tables, fields, field types and foreign keys
 * relationships.
 *
 * @author Kamil Bobrowski
 */
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
    return "DatabaseSchema{" + "graph=" + graph + '}';
  }

  public static class Builder {
    private Map<String, Map<String, NodeDefinition>> graph = new HashMap<>();

    public Builder() {}

    public DatabaseSchema build() {
      return new DatabaseSchema(graph);
    }

    /**
     * Add field to the table which is referencing another field.
     *
     * @param qualifiedName qualified name of the field in a form of "table/field"
     * @param type type of the field
     * @param qualifiedReferencingName qualified name of a field which is referenced by this field
     * @return builder of DatabaseSchema
     */
    public Builder row(String qualifiedName, FieldType type, String qualifiedReferencingName) {
      Objects.requireNonNull(qualifiedName);
      Objects.requireNonNull(type);
      Objects.requireNonNull(qualifiedReferencingName);
      addNode(
          new NodeDefinition(
              new QualifiedName(qualifiedName),
              type,
              new QualifiedName(qualifiedReferencingName),
              null));
      return this;
    }

    /**
     * Add field to the table.
     *
     * @param qualifiedName - qualified name of the field in a form of "table/field"
     * @param type - type of the field
     * @return builder of DatabaseSchema
     */
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
        addNode(
            new NodeDefinition(referencing, newNode.getFieldType(), null, Set.of(qualifiedName)));
      }
    }
  }

  private DatabaseSchema(Map<String, Map<String, NodeDefinition>> graph) {
    this.graph = graph;
  }
}
