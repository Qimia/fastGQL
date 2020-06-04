/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql.arguments;

import static dev.fastgql.common.FieldType.BOOL;
import static dev.fastgql.common.FieldType.FLOAT;
import static dev.fastgql.common.FieldType.INT;
import static dev.fastgql.common.FieldType.STRING;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import dev.fastgql.common.FieldType;
import graphql.schema.GraphQLScalarType;
import java.util.Map;

public class ArgumentsUtils {

  public static Map<FieldType, GraphQLScalarType> fieldTypeGraphQLScalarTypeMap =
      Map.of(
          INT, GraphQLInt,
          FLOAT, GraphQLFloat,
          STRING, GraphQLString,
          BOOL, GraphQLBoolean);
}
