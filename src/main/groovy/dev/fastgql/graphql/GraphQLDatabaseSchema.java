/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static graphql.Scalars.GraphQLInt;

import dev.fastgql.common.QualifiedName;
import dev.fastgql.common.ReferenceType;
import dev.fastgql.db.DatabaseSchema;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Data structure defining GraphQL schema, including standard fields as well as one-to-one and
 * one-to-many relationships between tables which are inferred from foreign keys. This class is
 * constructed from {@link DatabaseSchema} and is used it two ways: as a helper in building {@link
 * graphql.GraphQL} or as a helper in parsing {@link DataFetchingEnvironment}
 *
 * @author Kamil Bobrowski
 */
public class GraphQLDatabaseSchema {
  private final Map<String, Map<String, GraphQLField>> graph;
  private final DatabaseSchema databaseSchema;
  private final GraphQLArgument limit;
  private final GraphQLArgument offset;
  private final Map<String, GraphQLArgument> orderByMap;
  private final Map<String, GraphQLArgument> whereMap;

  public GraphQLField fieldAt(String table, String field) {
    return graph.get(table).get(field);
  }

  /**
   * Constructs object from {@link DatabaseSchema}.
   *
   * @param databaseSchema input schema
   */
  public GraphQLDatabaseSchema(DatabaseSchema databaseSchema) {
    this.graph = createGraph(databaseSchema);
    this.databaseSchema = databaseSchema;
    this.limit = createArgument("limit", GraphQLInt);
    this.offset = createArgument("offset", GraphQLInt);
    this.orderByMap = createOrderByMap(databaseSchema);
    this.whereMap = createWhereMap(databaseSchema);
  }

  /**
   * Applies this schema to given {@link GraphQLObjectType} Mutation object builder.
   *
   * @param builder builder to which this schema will be applied
   */
  public void applyMutation(GraphQLObjectType.Builder builder, boolean returningStatementEnabled) {
    Objects.requireNonNull(builder);
    databaseSchema
        .getGraph()
        .forEach(
            (tableName, keyNameToKeyDefinition) -> {
              GraphQLObjectType.Builder rowObjectBuilder =
                  GraphQLObjectType.newObject().name(String.format("%s_row", tableName));

              GraphQLInputObjectType.Builder rowInputBuilder =
                  GraphQLInputObjectType.newInputObject()
                      .name(String.format("%s_input", tableName));

              keyNameToKeyDefinition.forEach(
                  (keyName, keyDefinition) -> {
                    rowObjectBuilder.field(
                        GraphQLFieldDefinition.newFieldDefinition()
                            .name(keyName)
                            .type(GraphQLField.keyTypeToGraphQLType.get(keyDefinition.getKeyType()))
                            .build());
                    rowInputBuilder.field(
                        GraphQLInputObjectField.newInputObjectField()
                            .name(keyName)
                            .type(GraphQLField.keyTypeToGraphQLType.get(keyDefinition.getKeyType()))
                            .build());
                  });

              GraphQLObjectType rowObject = rowObjectBuilder.build();
              GraphQLInputObjectType rowInput = rowInputBuilder.build();

              GraphQLObjectType.Builder outputObjectBuilder =
                  GraphQLObjectType.newObject()
                      .name(String.format("%s_output", tableName))
                      .field(
                          GraphQLFieldDefinition.newFieldDefinition()
                              .name("affected_rows")
                              .type(GraphQLInt)
                              .build());
              if (returningStatementEnabled) {
                outputObjectBuilder.field(
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name("returning")
                        .type(GraphQLList.list(rowObject))
                        .build());
              }

              builder.field(
                  GraphQLFieldDefinition.newFieldDefinition()
                      .name(String.format("insert_%s", tableName))
                      .type(outputObjectBuilder)
                      .argument(
                          GraphQLArgument.newArgument()
                              .name("objects")
                              .type(GraphQLList.list(rowInput))
                              .build())
                      .build());
            });
  }

  /**
   * Applies this schema to given {@link GraphQLObjectType} builders (e.g. Query or Subscription
   * object builders).
   *
   * @param builders builders to which this schema will be applied
   */
  public void applyToGraphQLObjectTypes(List<GraphQLObjectType.Builder> builders) {
    Objects.requireNonNull(builders);
    graph.forEach(
        (tableName, fieldNameToGraphQLField) -> {
          GraphQLObjectType.Builder objectBuilder = GraphQLObjectType.newObject().name(tableName);
          fieldNameToGraphQLField.forEach(
              (fieldName, graphQLField) -> {
                GraphQLFieldDefinition.Builder fieldBuilder =
                    GraphQLFieldDefinition.newFieldDefinition()
                        .name(fieldName)
                        .type(graphQLField.getGraphQLType());
                if (graphQLField.getReferenceType() == ReferenceType.REFERENCED) {
                  String foreignTableName = graphQLField.getForeignName().getTableName();
                  fieldBuilder
                      .argument(limit)
                      .argument(offset)
                      .argument(orderByMap.get(foreignTableName))
                      .argument(whereMap.get(foreignTableName));
                }
                objectBuilder.field(fieldBuilder.build());
              });

          GraphQLObjectType object = objectBuilder.build();
          builders.forEach(
              builder ->
                  builder.field(
                      GraphQLFieldDefinition.newFieldDefinition()
                          .name(tableName)
                          .type(GraphQLList.list(object))
                          .argument(limit)
                          .argument(offset)
                          .argument(orderByMap.get(tableName))
                          .argument(whereMap.get(tableName))
                          .build()));
        });
  }

  private static GraphQLArgument createArgument(String name, GraphQLScalarType type) {
    return GraphQLArgument.newArgument().name(name).type(type).build();
  }

  private static Map<String, Map<String, GraphQLField>> createGraph(DatabaseSchema databaseSchema) {
    Objects.requireNonNull(databaseSchema);
    Map<String, Map<String, GraphQLField>> graph = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (tableName, keyNameToKeyDefinition) -> {
              graph.put(tableName, new HashMap<>());
              Map<String, GraphQLField> fieldNameToGraphQLFieldDefinition = graph.get(tableName);
              keyNameToKeyDefinition.forEach(
                  (keyName, keyDefinition) -> {
                    QualifiedName qualifiedName = keyDefinition.getQualifiedName();
                    QualifiedName referencing = keyDefinition.getReferencing();
                    Set<QualifiedName> referencedBySet = keyDefinition.getReferencedBy();
                    fieldNameToGraphQLFieldDefinition.put(
                        keyName,
                        GraphQLField.createLeaf(qualifiedName, keyDefinition.getKeyType()));
                    if (referencing != null) {
                      String referencingName =
                          GraphQLNaming.getNameForReferencingField(qualifiedName);
                      fieldNameToGraphQLFieldDefinition.put(
                          referencingName,
                          GraphQLField.createReferencing(qualifiedName, referencing));
                    }
                    referencedBySet.forEach(
                        referencedBy -> {
                          String referencedByName =
                              GraphQLNaming.getNameForReferencedByField(referencedBy);
                          fieldNameToGraphQLFieldDefinition.put(
                              referencedByName,
                              GraphQLField.createReferencedBy(qualifiedName, referencedBy));
                        });
                  });
            });
    return graph;
  }

  private static Map<String, GraphQLArgument> createOrderByMap(DatabaseSchema databaseSchema) {
    Map<String, GraphQLArgument> orderByMap = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (parent, subGraph) -> {
              String orderByName = GraphQLNaming.getNameOrderByType(parent);
              GraphQLInputObjectType.Builder builder =
                  GraphQLInputObjectType.newInputObject().name(orderByName);
              subGraph.forEach(
                  (name, node) -> {
                    builder.field(
                        GraphQLInputObjectField.newInputObjectField()
                            .name(name)
                            .type(OrderBy.enumType)
                            .build());
                    // if node is referencing, add schema field referencing to corresponding schema
                    // type
                    if (node.getReferencing() != null) {
                      String referencingName =
                          GraphQLNaming.getNameForReferencingField(node.getQualifiedName());
                      String referencingTypeName =
                          GraphQLNaming.getNameOrderByType(node.getReferencing().getTableName());
                      builder.field(
                          GraphQLInputObjectField.newInputObjectField()
                              .name(referencingName)
                              .type(GraphQLTypeReference.typeRef(referencingTypeName))
                              .build());
                    }
                  });
              GraphQLInputType orderByType = GraphQLList.list(builder.build());
              // create argument
              GraphQLArgument orderBy =
                  GraphQLArgument.newArgument().name("order_by").type(orderByType).build();
              orderByMap.put(parent, orderBy);
            });
    return orderByMap;
  }

  private static Map<String, GraphQLArgument> createWhereMap(DatabaseSchema databaseSchema) {
    Map<String, GraphQLArgument> whereMap = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (parent, subGraph) -> {
              String whereName = GraphQLNaming.getNameBoolType(parent);
              GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
              builder
                  .name(whereName)
                  .description(
                      "Boolean expression to filter rows from the table \""
                          + parent
                          + "\". All fields are combined with a logical 'AND'.")
                  .field(
                      GraphQLInputObjectField.newInputObjectField()
                          .name("_and")
                          .type(GraphQLList.list(GraphQLTypeReference.typeRef(whereName)))
                          .build())
                  .field(
                      GraphQLInputObjectField.newInputObjectField()
                          .name("_not")
                          .type(GraphQLTypeReference.typeRef(whereName))
                          .build())
                  .field(
                      GraphQLInputObjectField.newInputObjectField()
                          .name("_or")
                          .type(GraphQLList.list(GraphQLTypeReference.typeRef(whereName)))
                          .build());
              subGraph.forEach(
                  (name, node) -> {
                    GraphQLInputType nodeType =
                        ConditionalOperatorTypes.scalarTypeToComparisonExpMap.get(
                            GraphQLField.keyTypeToGraphQLType.get(node.getKeyType()));
                    builder.field(
                        GraphQLInputObjectField.newInputObjectField()
                            .name(name)
                            .type(nodeType)
                            .build());
                    if (node.getReferencing() != null) {
                      String referencingName =
                          GraphQLNaming.getNameForReferencingField(node.getQualifiedName());
                      String referencingTypeName =
                          GraphQLNaming.getNameBoolType(node.getReferencing().getTableName());
                      builder.field(
                          GraphQLInputObjectField.newInputObjectField()
                              .name(referencingName)
                              .type(GraphQLTypeReference.typeRef(referencingTypeName))
                              .build());
                    }
                  });
              GraphQLInputType whereType = builder.build();
              GraphQLArgument where =
                  GraphQLArgument.newArgument().name("where").type(whereType).build();
              whereMap.put(parent, where);
            });
    return whereMap;
  }

  @Override
  public String toString() {
    return "GraphQLDatabaseSchema{"
        + "graph="
        + graph
        + ", limit="
        + limit
        + ", offset="
        + offset
        + ", orderByMap="
        + orderByMap
        + ", whereMap="
        + whereMap
        + '}';
  }
}
