/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data structure defining database schema, including tables, keys, key types and foreign key
 * relationships.
 *
 * @author Kamil Bobrowski
 */
public class DatabaseSchema {
  private final Map<String, Map<String, KeyDefinition>> graph;

  private DatabaseSchema(Map<String, Map<String, KeyDefinition>> graph) {
    this.graph = graph;
  }

  public static DatabaseSchema.Builder newSchema() {
    return new DatabaseSchema.Builder();
  }

  public Map<String, Map<String, KeyDefinition>> getGraph() {
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
    private Map<String, Map<String, KeyDefinition>> graph = new HashMap<>();

    public Builder() {}

    public DatabaseSchema build() {
      return new DatabaseSchema(graph);
    }

    /**
     * Add key to the table which is referencing another key.
     *
     * @param qualifiedName qualified name of the key in a form of "table/key"
     * @param type type of the key
     * @param qualifiedReferencingName qualified name of a key which is referenced by this key
     * @return builder of DatabaseSchema
     */
    public Builder addKey(String qualifiedName, KeyType type, String qualifiedReferencingName) {
      Objects.requireNonNull(qualifiedName);
      Objects.requireNonNull(type);
      Objects.requireNonNull(qualifiedReferencingName);
      addKeyDefinition(
          new KeyDefinition(
              new QualifiedName(qualifiedName),
              type,
              new QualifiedName(qualifiedReferencingName),
              null));
      return this;
    }

    /**
     * Add key to the table.
     *
     * @param qualifiedName - qualified name of the key in a form of "table/key"
     * @param type - type of the key
     * @return builder of DatabaseSchema
     */
    public Builder addKey(String qualifiedName, KeyType type) {
      Objects.requireNonNull(qualifiedName);
      Objects.requireNonNull(type);
      addKeyDefinition(new KeyDefinition(new QualifiedName(qualifiedName), type, null, null));
      return this;
    }

    private KeyDefinition keyAt(QualifiedName qualifiedName) {
      String parent = qualifiedName.getTableName();
      String name = qualifiedName.getKeyName();
      if (graph.containsKey(parent) && graph.get(parent).containsKey(name)) {
        return graph.get(parent).get(name);
      } else {
        return null;
      }
    }

    private void mergeKeyAt(QualifiedName qualifiedName, KeyDefinition node) {
      Objects.requireNonNull(keyAt(qualifiedName)).merge(node);
    }

    private void addKeyAt(QualifiedName qualifiedName, KeyDefinition node) {
      String parent = qualifiedName.getTableName();
      String name = qualifiedName.getKeyName();
      if (!graph.containsKey(parent)) {
        graph.put(parent, new HashMap<>());
      }
      graph.get(parent).put(name, node);
    }

    private void addKeyDefinition(KeyDefinition keyDefinition) {
      QualifiedName qualifiedName = keyDefinition.getQualifiedName();
      if (keyAt(qualifiedName) != null) {
        mergeKeyAt(qualifiedName, keyDefinition);
      } else {
        addKeyAt(qualifiedName, keyDefinition);
      }
      QualifiedName referencing = keyDefinition.getReferencing();
      if (referencing != null) {
        addKeyDefinition(
            new KeyDefinition(
                referencing, keyDefinition.getKeyType(), null, Set.of(qualifiedName)));
      }
    }
  }
}
