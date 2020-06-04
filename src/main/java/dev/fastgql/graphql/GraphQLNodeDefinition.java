/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import dev.fastgql.common.FieldType;
import dev.fastgql.common.QualifiedName;
import dev.fastgql.common.ReferenceType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import java.util.Map;

public class GraphQLNodeDefinition {
  private final QualifiedName qualifiedName;
  private final GraphQLOutputType graphQLType;
  private final QualifiedName foreignName;
  private final ReferenceType referenceType;
  private static final Map<FieldType, GraphQLScalarType> nodeToGraphQLType =
      Map.of(
          FieldType.INT, GraphQLInt,
          FieldType.STRING, GraphQLString,
          FieldType.FLOAT, GraphQLFloat);

  public static GraphQLNodeDefinition createLeaf(QualifiedName qualifiedName, FieldType type) {
    return new GraphQLNodeDefinition(
        qualifiedName, nodeToGraphQLType.get(type), null, ReferenceType.NONE);
  }

  public static GraphQLNodeDefinition createReferencing(
      QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(
        qualifiedName,
        GraphQLTypeReference.typeRef(foreignName.getParent()),
        foreignName,
        ReferenceType.REFERENCING);
  }

  public static GraphQLNodeDefinition createReferencedBy(
      QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(
        qualifiedName,
        GraphQLList.list(GraphQLTypeReference.typeRef(foreignName.getParent())),
        foreignName,
        ReferenceType.REFERENCED);
  }

  public QualifiedName getQualifiedName() {
    return qualifiedName;
  }

  public GraphQLOutputType getGraphQLType() {
    return graphQLType;
  }

  public QualifiedName getForeignName() {
    return foreignName;
  }

  public ReferenceType getReferenceType() {
    return referenceType;
  }

  private GraphQLNodeDefinition(
      QualifiedName qualifiedName,
      GraphQLOutputType graphQLType,
      QualifiedName foreignName,
      ReferenceType referenceType) {
    this.qualifiedName = qualifiedName;
    this.graphQLType = graphQLType;
    this.foreignName = foreignName;
    this.referenceType = referenceType;
  }

  @Override
  public String toString() {
    return "GraphQLNodeDefinition{"
        + "qualifiedName="
        + qualifiedName
        + ", graphQLType="
        + graphQLType
        + ", foreignName="
        + foreignName
        + '}';
  }
}
