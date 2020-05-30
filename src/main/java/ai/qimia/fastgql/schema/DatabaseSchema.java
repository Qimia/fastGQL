package ai.qimia.fastgql.schema;

import graphql.schema.*;
import java.util.*;
import static graphql.Scalars.*;

public class DatabaseSchema {
  private final Map<String, Map<String, NodeDefinition>> graph;
  private static final Map<FieldType, GraphQLScalarType> nodeToGraphQLType = Map.of(
    FieldType.INT, GraphQLInt,
    FieldType.STRING, GraphQLString,
    FieldType.FLOAT, GraphQLFloat
  );

  public static DatabaseSchema.Builder newSchema() {
    return new DatabaseSchema.Builder();
  }

  public void addToGraphQLObjectType(GraphQLObjectType.Builder builder) {
    Objects.requireNonNull(builder);
    graph.forEach((parent, subgraph) -> {
      GraphQLObjectType.Builder object = GraphQLObjectType.newObject()
        .name(parent);
      subgraph.forEach((name, node) -> {
        object.field(GraphQLFieldDefinition.newFieldDefinition()
          .name(name)
          .type(nodeToGraphQLType.get(node.getFieldType()))
          .build());
        QualifiedName referencing = node.getReferencing();
        Set<QualifiedName> referredBySet = node.getReferredBy();
        if (referencing != null) {
          object.field(GraphQLFieldDefinition.newFieldDefinition()
            .name(getNameForReferencingField(node.getQualifiedName()))
            .type(GraphQLTypeReference.typeRef(referencing.getParent()))
            .build());
        }
        referredBySet.forEach(referredBy -> {
          object.field(GraphQLFieldDefinition.newFieldDefinition()
            .name(getNameForReferredByField(referredBy))
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(referredBy.getParent())))
            .build());
        });
      });
      builder.field(GraphQLFieldDefinition.newFieldDefinition()
        .name(parent)
        .type(GraphQLList.list(object.build()))
        .build());
    });
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

  private String getNameForReferencingField(QualifiedName qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    return String.format("%s_ref", qualifiedName.getName());
  }

  private String getNameForReferredByField(QualifiedName qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    return String.format("%s_on_%s", qualifiedName.getParent(), qualifiedName.getName());
  }
}
