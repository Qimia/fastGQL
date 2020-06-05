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

/**
 * Definition of single field in GraphQL.
 *
 * @author Kamil Bobrowski
 */
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

  /**
   * Create a field which extracts value for given field in a table. This type of field
   * in GraphQL query will just return a single value.
   *
   * @param qualifiedName qualified name of the field
   * @param type type of the field
   * @return new field definition
   */
  public static GraphQLNodeDefinition createLeaf(QualifiedName qualifiedName, FieldType type) {
    return new GraphQLNodeDefinition(
        qualifiedName, nodeToGraphQLType.get(type), null, ReferenceType.NONE);
  }

  /**
   * Create a field which is referencing other field. This type of field in GraphQL query
   * will return a matching table being referenced.
   *
   * @param qualifiedName qualified name of the field which is referencing other field
   * @param foreignName qualified name of the field being referenced
   * @return new field definition
   */
  public static GraphQLNodeDefinition createReferencing(
      QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(
        qualifiedName,
        GraphQLTypeReference.typeRef(foreignName.getParent()),
        foreignName,
        ReferenceType.REFERENCING);
  }

  /**
   * Create a field which is referenced by other field. This type of field in GraphQL query
   * will return a list of tables which are referencing this field.
   *
   * @param qualifiedName qualified name of the field which is being referenced
   * @param foreignName qualified
   * @return new field definition
   */
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
