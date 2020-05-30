package ai.qimia.fastgql.schema;

import graphql.schema.*;

import java.util.Map;

import static graphql.Scalars.*;

public class GraphQLNodeDefinition {
  private final QualifiedName qualifiedName;
  private final GraphQLOutputType graphQLType;
  private final QualifiedName foreignName;
  private static final Map<FieldType, GraphQLScalarType> nodeToGraphQLType = Map.of(
    FieldType.INT, GraphQLInt,
    FieldType.STRING, GraphQLString,
    FieldType.FLOAT, GraphQLFloat
  );

  public static GraphQLNodeDefinition createLeaf(QualifiedName qualifiedName, FieldType type) {
    return new GraphQLNodeDefinition(qualifiedName, nodeToGraphQLType.get(type), null);
  }

  public static GraphQLNodeDefinition createReferencing(QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(qualifiedName, GraphQLTypeReference.typeRef(foreignName.getParent()), foreignName);
  }

  public static GraphQLNodeDefinition createReferencedBy(QualifiedName qualifiedName, QualifiedName foreignName) {
    return new GraphQLNodeDefinition(qualifiedName, GraphQLList.list(GraphQLTypeReference.typeRef(foreignName.getParent())), foreignName);
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

  private GraphQLNodeDefinition(QualifiedName qualifiedName, GraphQLOutputType graphQLType, QualifiedName foreignName) {
    this.qualifiedName = qualifiedName;
    this.graphQLType = graphQLType;
    this.foreignName = foreignName;
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
