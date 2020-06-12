/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import dev.fastgql.common.KeyType;
import graphql.schema.GraphQLScalarType;
import java.util.Map;

public class GraphQLArgumentsUtils {

  public static Map<KeyType, GraphQLScalarType> fieldTypeGraphQLScalarTypeMap =
      Map.of(
          KeyType.INT, GraphQLInt,
          KeyType.FLOAT, GraphQLFloat,
          KeyType.STRING, GraphQLString,
          KeyType.BOOL, GraphQLBoolean);
}
