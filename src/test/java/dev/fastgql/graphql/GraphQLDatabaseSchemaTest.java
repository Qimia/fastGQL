/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static graphql.Scalars.GraphQLInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.fastgql.common.KeyType;
import dev.fastgql.db.DatabaseSchema;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLObjectType.Builder;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GraphQLDatabaseSchemaTest {

  private GraphQLDatabaseSchema graphQLDatabaseSchema;

  @BeforeEach
  public void setUp() {
    graphQLDatabaseSchema =
        new GraphQLDatabaseSchema(
            DatabaseSchema.newSchema()
                .addKey("customers/id", KeyType.INT)
                .addKey("customers/address", KeyType.INT, "addresses/id")
                .addKey("addresses/id", KeyType.INT)
                .build());
  }

  @Test
  public void applyToGraphQLObjectTypes() {
    List<Builder> builders = new ArrayList<>();
    GraphQLObjectType.Builder testBuilder = GraphQLObjectType.newObject().name("test");
    builders.add(testBuilder);
    graphQLDatabaseSchema.applyToGraphQLObjectTypes(builders);
    GraphQLObjectType graphQLSchemaObject = builders.get(0).build();
    assertEquals(2, graphQLSchemaObject.getFieldDefinitions().size());
    assertArgumentsInFieldDefinition(graphQLSchemaObject, "addresses", "addresses");
    assertArgumentsInFieldDefinition(graphQLSchemaObject, "customers", "customers");
    GraphQLObjectType customersObjectType =
        (GraphQLObjectType)
            graphQLSchemaObject.getFieldDefinition("customers").getType().getChildren().get(0);
    List.of("id", "address_ref", "address")
        .forEach(name -> assertNotNull(customersObjectType.getFieldDefinition(name)));
    GraphQLObjectType addressesObjectType =
        (GraphQLObjectType)
            graphQLSchemaObject.getFieldDefinition("addresses").getType().getChildren().get(0);
    assertNotNull(addressesObjectType.getFieldDefinition("id"));
    assertArgumentsInFieldDefinition(addressesObjectType, "customers_on_address", "customers");
  }

  private void assertArgumentsInFieldDefinition(
      GraphQLObjectType graphQLObjectType, String name, String typeName) {
    GraphQLFieldDefinition graphQLFieldDefinition = graphQLObjectType.getFieldDefinition(name);
    assertNotNull(graphQLFieldDefinition);
    assertEquals(typeName, graphQLFieldDefinition.getType().getChildren().get(0).getName());
    assertEquals(4, graphQLFieldDefinition.getArguments().size());
    assertEquals(GraphQLInt, graphQLFieldDefinition.getArgument("limit").getType());
    assertEquals(GraphQLInt, graphQLFieldDefinition.getArgument("offset").getType());
    assertEquals(
        String.format("%s_order_by", typeName),
        graphQLFieldDefinition.getArgument("order_by").getType().getChildren().get(0).getName());
    assertEquals(
        String.format("%s_bool_exp", typeName),
        graphQLFieldDefinition.getArgument("where").getType().getName());
  }
}
