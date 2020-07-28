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
public class GraphQLField {
  public static final Map<KeyType, GraphQLScalarType> keyTypeToGraphQLType =
      Map.of(
          KeyType.INT, GraphQLInt,
          KeyType.STRING, GraphQLString,
          KeyType.FLOAT, GraphQLFloat,
          KeyType.BOOL, GraphQLBoolean);
  private final QualifiedName qualifiedName;
  private final GraphQLOutputType graphQLType;
  private final QualifiedName foreignName;
  private final ReferenceType referenceType;

  private GraphQLField(
      QualifiedName qualifiedName,
      GraphQLOutputType graphQLType,
      QualifiedName foreignName,
      ReferenceType referenceType) {
    this.qualifiedName = qualifiedName;
    this.graphQLType = graphQLType;
    this.foreignName = foreignName;
    this.referenceType = referenceType;
  }

  /**
   * Create a field which extracts value for given key in a table. This type of field in GraphQL
   * query will just return a single value.
   *
   * @param qualifiedName qualified name of the key
   * @param type type of the key
   * @return new field definition
   */
  public static GraphQLField createLeaf(QualifiedName qualifiedName, KeyType type) {
    return new GraphQLField(
        qualifiedName, keyTypeToGraphQLType.get(type), null, ReferenceType.NONE);
  }

  /**
   * Create a field which is referencing other table. This type of field in GraphQL query will
   * return a matching table being referenced.
   *
   * @param qualifiedName qualified name of the key which is referencing other key
   * @param foreignName qualified name of the key being referenced
   * @return new field definition
   */
  public static GraphQLField createReferencing(
      QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLField(
        qualifiedName,
        GraphQLTypeReference.typeRef(foreignName.getTableName()),
        foreignName,
        ReferenceType.REFERENCING);
  }

  /**
   * Create a field which is referenced by other tables. This type of field in GraphQL query will
   * return a list of tables which are referencing this field.
   *
   * @param qualifiedName qualified name of the key which is being referenced
   * @param foreignName qualified name of foreign key which references this key
   * @return new field definition
   */
  public static GraphQLField createReferencedBy(
      QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLField(
        qualifiedName,
        GraphQLList.list(GraphQLTypeReference.typeRef(foreignName.getTableName())),
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
