/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql.arguments;

import static dev.fastgql.graphql.GraphQLFieldDefinition.keyTypeToGraphQLType;
import static dev.fastgql.graphql.GraphQLNaming.getNameBoolType;
import static dev.fastgql.graphql.GraphQLNaming.getNameForReferencingField;
import static graphql.Scalars.GraphQLInt;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.graphql.GraphQLNaming;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLTypeReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to construct GraphQL arguments for GraphQL database schemas.
 *
 * @author Mingyi Zhang
 */
public class GraphQLArguments {
  private final GraphQLArgument limit;
  private final GraphQLArgument offset;
  private final Map<String, GraphQLArgument> orderBys;
  private final Map<String, GraphQLArgument> wheres;

  public GraphQLArguments(DatabaseSchema databaseSchema) {
    this.limit = GraphQLArgument.newArgument().name("limit").type(GraphQLInt).build();
    this.offset = GraphQLArgument.newArgument().name("offset").type(GraphQLInt).build();
    this.orderBys = createOrderBys(databaseSchema);
    this.wheres = createWheres(databaseSchema);
  }

  public GraphQLArgument getLimit() {
    return limit;
  }

  public GraphQLArgument getOffset() {
    return offset;
  }

  public Map<String, GraphQLArgument> getOrderBys() {
    return orderBys;
  }

  public Map<String, GraphQLArgument> getWheres() {
    return wheres;
  }

  public GraphQLArgument getOrderByFor(String foreignTableName) {
    return orderBys.get(foreignTableName);
  }

  public GraphQLArgument getWhereFor(String foreignTableName) {
    return wheres.get(foreignTableName);
  }

  private Map<String, GraphQLArgument> createOrderBys(DatabaseSchema databaseSchema) {
    Map<String, GraphQLArgument> orderBys = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (tableName, keyNameToKeyDefinition) -> {
              String orderByName = GraphQLNaming.getNameOrderByType(tableName);
              GraphQLInputObjectType.Builder builder =
                  GraphQLInputObjectType.newInputObject().name(orderByName);
              keyNameToKeyDefinition.forEach(
                  (keyName, keyDefinition) -> {
                    builder.field(
                        GraphQLInputObjectField.newInputObjectField()
                            .name(keyName)
                            .type(OrderBy.enumType)
                            .build());
                    // if keyDefinition is referencing, add schema field referencing to
                    // corresponding schema
                    // type
                    if (keyDefinition.getReferencing() != null) {
                      String referencingName =
                          getNameForReferencingField(keyDefinition.getQualifiedName());
                      String referencingTypeName =
                          GraphQLNaming.getNameOrderByType(
                              keyDefinition.getReferencing().getTableName());
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
              orderBys.put(tableName, orderBy);
            });
    return orderBys;
  }

  private Map<String, GraphQLArgument> createWheres(DatabaseSchema databaseSchema) {
    Map<String, GraphQLArgument> wheres = new HashMap<>();
    databaseSchema
        .getGraph()
        .forEach(
            (tableName, keyNameToKeyDefinition) -> {
              String whereName = getNameBoolType(tableName);
              GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
              builder
                  .name(whereName)
                  .description(
                      "Boolean expression to filter rows from the table \""
                          + tableName
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
              keyNameToKeyDefinition.forEach(
                  (keyName, keyDefinition) -> {
                    GraphQLInputType nodeType =
                        ConditionalOperatorTypes.scalarTypeToComparisonExpMap.get(
                            keyTypeToGraphQLType.get(keyDefinition.getKeyType()));
                    builder.field(
                        GraphQLInputObjectField.newInputObjectField()
                            .name(keyName)
                            .type(nodeType)
                            .build());
                    if (keyDefinition.getReferencing() != null) {
                      String referencingName =
                          getNameForReferencingField(keyDefinition.getQualifiedName());
                      String referencingTypeName =
                          getNameBoolType(keyDefinition.getReferencing().getTableName());
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
              wheres.put(tableName, where);
            });
    return wheres;
  }
}
