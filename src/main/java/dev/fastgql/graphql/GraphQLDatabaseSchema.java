/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import dev.fastgql.common.QualifiedName;
import dev.fastgql.db.DatabaseSchema;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data structure defining GraphQL schema, including standard fields as well as one-to-one and
 * one-to-many relationships between tables which are inferred from foreign keys. This class is
 * constructed from {@link DatabaseSchema} and is used it two ways: as a helper in building {@link
 * graphql.GraphQL} or as a helper in parsing {@link graphql.schema.DataFetchingEnvironment}
 *
 * @author Kamil Bobrowski
 */
public class GraphQLDatabaseSchema {
  private Map<String, Map<String, GraphQLFieldDefinition>> graph;

  private String getNameForReferencingField(QualifiedName qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    return String.format("%s_ref", qualifiedName.getKeyName());
  }

  private String getNameForReferencedByField(QualifiedName qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    return String.format("%s_on_%s", qualifiedName.getTableName(), qualifiedName.getKeyName());
  }

  public GraphQLFieldDefinition fieldAt(String table, String field) {
    return graph.get(table).get(field);
  }

  /**
   * Constructs object from {@link DatabaseSchema}.
   *
   * @param databaseSchema input schema
   */
  public GraphQLDatabaseSchema(DatabaseSchema databaseSchema) {
    Objects.requireNonNull(databaseSchema);
    graph = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (tableName, keyNameToKeyDefinition) -> {
              graph.put(tableName, new HashMap<>());
              Map<String, GraphQLFieldDefinition> fieldNameToGraphQLFieldDefinition =
                  graph.get(tableName);
              keyNameToKeyDefinition.forEach(
                  (keyName, keyDefinition) -> {
                    QualifiedName qualifiedName = keyDefinition.getQualifiedName();
                    QualifiedName referencing = keyDefinition.getReferencing();
                    Set<QualifiedName> referencedBySet = keyDefinition.getReferencedBy();
                    fieldNameToGraphQLFieldDefinition.put(
                        keyName,
                        GraphQLFieldDefinition.createLeaf(
                            qualifiedName, keyDefinition.getKeyType()));
                    if (referencing != null) {
                      String referencingName = getNameForReferencingField(qualifiedName);
                      fieldNameToGraphQLFieldDefinition.put(
                          referencingName,
                          GraphQLFieldDefinition.createReferencing(qualifiedName, referencing));
                    }
                    referencedBySet.forEach(
                        referencedBy -> {
                          String referencedByName = getNameForReferencedByField(referencedBy);
                          fieldNameToGraphQLFieldDefinition.put(
                              referencedByName,
                              GraphQLFieldDefinition.createReferencedBy(
                                  qualifiedName, referencedBy));
                        });
                  });
            });
  }

  /**
   * Applies this schema to given {@link GraphQLObjectType} builders (e.g. Query or Subscription
   * object builders). Has to be done this way since internally it constructs other {@link
   * GraphQLObjectType}, which should be constructed only once with the same name.
   *
   * @param builders builders to which this schema will be applied
   */
  public void applyToGraphQLObjectTypes(List<GraphQLObjectType.Builder> builders) {
    Objects.requireNonNull(builders);
    graph.forEach(
        (tableName, fieldNameToGraphQLFieldDefinition) -> {
          GraphQLObjectType.Builder objectBuilder = GraphQLObjectType.newObject().name(tableName);
          fieldNameToGraphQLFieldDefinition.forEach(
              (fieldName, graphQLFieldDefinition) -> {
                objectBuilder.field(
                    graphql.schema.GraphQLFieldDefinition.newFieldDefinition()
                        .name(fieldName)
                        .type(graphQLFieldDefinition.getGraphQLType())
                        .build());
              });
          GraphQLObjectType object = objectBuilder.build();
          builders.forEach(
              builder ->
                  builder.field(
                      graphql.schema.GraphQLFieldDefinition.newFieldDefinition()
                          .name(tableName)
                          .type(GraphQLList.list(object))
                          .build()));
        });
  }

  @Override
  public String toString() {
    return "GraphQLDatabaseSchema{" + "graph=" + graph + '}';
  }
}
