/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static dev.fastgql.graphql.GraphQLNaming.getNameForReferencedByField;
import static dev.fastgql.graphql.GraphQLNaming.getNameForReferencingField;

import dev.fastgql.common.QualifiedName;
import dev.fastgql.common.ReferenceType;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.graphql.arguments.GraphQLArguments;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class GraphQLDatabaseSchema {
  private Map<String, Map<String, GraphQLNodeDefinition>> graph;

  public GraphQLDatabaseSchema(DatabaseSchema databaseSchema) {
    Objects.requireNonNull(databaseSchema);
    graph = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (parent, subgraph) -> {
              graph.put(parent, new HashMap<>());
              Map<String, GraphQLNodeDefinition> graphQLSubgraph = graph.get(parent);
              subgraph.forEach(
                  (name, node) -> {
                    QualifiedName qualifiedName = node.getQualifiedName();
                    QualifiedName referencing = node.getReferencing();
                    Set<QualifiedName> referencedBySet = node.getReferencedBy();
                    graphQLSubgraph.put(
                        name, GraphQLNodeDefinition.createLeaf(qualifiedName, node.getFieldType()));
                    if (referencing != null) {
                      String referencingName = getNameForReferencingField(qualifiedName);
                      graphQLSubgraph.put(
                          referencingName,
                          GraphQLNodeDefinition.createReferencing(qualifiedName, referencing));
                    }
                    referencedBySet.forEach(
                        referencedBy -> {
                          String referencedByName = getNameForReferencedByField(referencedBy);
                          graphQLSubgraph.put(
                              referencedByName,
                              GraphQLNodeDefinition.createReferencedBy(
                                  qualifiedName, referencedBy));
                        });
                  });
            });
  }

  public GraphQLNodeDefinition nodeAt(String table, String field) {
    return graph.get(table).get(field);
  }

  public void applyToGraphQLObjectTypes(
      List<GraphQLObjectType.Builder> builders, GraphQLArguments args) {
    Objects.requireNonNull(builders);
    graph.forEach(
        (parent, subgraph) -> {
          GraphQLObjectType.Builder objectBuilder = GraphQLObjectType.newObject().name(parent);
          subgraph.forEach(
              (name, node) -> {
                GraphQLFieldDefinition.Builder subBuilder =
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(name)
                        .type(node.getGraphQLType());
                if (node.getReferenceType() == ReferenceType.REFERENCED) {
                  String parentName = node.getForeignName().getParent();
                  subBuilder
                      .argument(args.getLimit())
                      .argument(args.getOffset())
                      .argument(args.getOrderBys().get(parentName))
                      .argument(args.getWheres().get(parentName));
                }
                objectBuilder.field(subBuilder.build());
              });

          GraphQLObjectType object = objectBuilder.build();
          builders.forEach(
              builder ->
                  builder.field(
                      GraphQLFieldDefinition.newFieldDefinition()
                          .name(parent)
                          .type(GraphQLList.list(object))
                          .argument(args.getLimit())
                          .argument(args.getOffset())
                          .argument(args.getOrderBys().get(parent))
                          .argument(args.getWheres().get(parent))
                          .build()));
        });
  }

  @Override
  public String toString() {
    return "GraphQLSchema{" + "graph=" + graph + '}';
  }
}
