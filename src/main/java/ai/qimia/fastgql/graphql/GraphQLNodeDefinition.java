package ai.qimia.fastgql.graphql;

import ai.qimia.fastgql.common.FieldType;
import ai.qimia.fastgql.common.QualifiedName;
import graphql.schema.*;

import java.util.Map;

import static graphql.Scalars.*;

public class GraphQLNodeDefinition {
  private final QualifiedName qualifiedName;
  private final GraphQLOutputType graphQLType;
  private final QualifiedName foreignName;
  private final ReferenceType referenceType;
  private static final Map<FieldType, GraphQLScalarType> nodeToGraphQLType = Map.of(
    FieldType.INT, GraphQLInt,
    FieldType.STRING, GraphQLString,
    FieldType.FLOAT, GraphQLFloat
  );

  public static GraphQLNodeDefinition createLeaf(QualifiedName qualifiedName, FieldType type) {
    return new GraphQLNodeDefinition(qualifiedName, nodeToGraphQLType.get(type), null, ReferenceType.NONE);
  }

  public static GraphQLNodeDefinition createReferencing(QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(qualifiedName, GraphQLTypeReference.typeRef(foreignName.getParent()), foreignName, ReferenceType.REFERENCING);
  }

  public static GraphQLNodeDefinition createReferencedBy(QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(qualifiedName, GraphQLList.list(GraphQLTypeReference.typeRef(foreignName.getParent())), foreignName, ReferenceType.REFERENCED);
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

  private GraphQLNodeDefinition(QualifiedName qualifiedName, GraphQLOutputType graphQLType, QualifiedName foreignName, ReferenceType referenceType) {
    this.qualifiedName = qualifiedName;
    this.graphQLType = graphQLType;
    this.foreignName = foreignName;
    this.referenceType = referenceType;
  }

  @Override
  public String toString() {
    return "GraphQLNodeDefinition{" +
      "qualifiedName=" + qualifiedName +
      ", graphQLType=" + graphQLType +
      ", foreignName=" + foreignName +
      '}';
  }
}
