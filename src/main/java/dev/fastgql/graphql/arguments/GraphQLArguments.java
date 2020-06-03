/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql.arguments;

import static graphql.Scalars.GraphQLInt;

import dev.fastgql.db.DatabaseSchema;
import graphql.schema.GraphQLArgument;

public class GraphQLArguments {
  private final GraphQLArgument limit;
  private final GraphQLArgument offset;
  public GraphQLArguments(DatabaseSchema databaseSchema) {
    this.limit = GraphQLArgument.newArgument().name("limit").type(GraphQLInt).build();
    this.offset = GraphQLArgument.newArgument().name("offset").type(GraphQLInt).build();
  }

  public GraphQLArgument getLimit() {
    return limit;
  }

  public GraphQLArgument getOffset() {
    return offset;
  }
}
