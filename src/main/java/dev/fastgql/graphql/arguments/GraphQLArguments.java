/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql.arguments;

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

public class GraphQLArguments {
  private final GraphQLArgument limit;
  private final GraphQLArgument offset;
  private final Map<String, GraphQLArgument> orderBys;

  public GraphQLArguments(DatabaseSchema databaseSchema) {
    this.limit = GraphQLArgument.newArgument().name("limit").type(GraphQLInt).build();
    this.offset = GraphQLArgument.newArgument().name("offset").type(GraphQLInt).build();
    this.orderBys = createOrderBys(databaseSchema);
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

  private Map<String, GraphQLArgument> createOrderBys(DatabaseSchema databaseSchema) {
    Map<String, GraphQLArgument> orderBys = new HashMap<>();
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
                    // if node is referencing, add schema field referencing to corresponding schema type
                    if (node.getReferencing() != null) {
                      String referencingName =
                          GraphQLNaming.getNameForReferencingField(node.getQualifiedName());
                      String referencingTypeName =
                          GraphQLNaming.getNameOrderByType(node.getReferencing().getParent());
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
              orderBys.put(parent, orderBy);
            });
    return orderBys;
  }
}
