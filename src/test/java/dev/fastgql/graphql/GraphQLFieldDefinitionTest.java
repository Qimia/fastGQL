/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static graphql.Scalars.GraphQLFloat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import dev.fastgql.common.ReferenceType;
import org.junit.jupiter.api.Test;

public class GraphQLFieldDefinitionTest {

  @Test
  public void shouldCreateLeaf() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    GraphQLFieldDefinition actual = GraphQLFieldDefinition.createLeaf(qualifiedName, KeyType.INT);
    assertEquals(qualifiedName, actual.getQualifiedName());
    assertEquals(GraphQLFloat, actual.getGraphQLType());
    assertNull(actual.getForeignName());
    assertEquals(ReferenceType.NONE, actual.getReferenceType());
  }

  @Test
  public void shouldCreateReferencing() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    QualifiedName foreignName = new QualifiedName("foreignTableName/foreignKeyName");
    GraphQLFieldDefinition actual =
        GraphQLFieldDefinition.createReferencing(qualifiedName, foreignName);
    assertEquals(qualifiedName, actual.getQualifiedName());
    assertEquals(foreignName, actual.getForeignName());
    assertEquals(ReferenceType.REFERENCING, actual.getReferenceType());
    assertEquals("foreignTableName", actual.getGraphQLType().getName());
  }

  @Test
  public void shouldCreateReferencedBy() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    QualifiedName foreignName = new QualifiedName("foreignTableName/foreignKeyName");
    GraphQLFieldDefinition actual =
        GraphQLFieldDefinition.createReferencedBy(qualifiedName, foreignName);
    assertEquals(qualifiedName, actual.getQualifiedName());
    assertEquals(foreignName, actual.getForeignName());
    assertEquals(ReferenceType.REFERENCED, actual.getReferenceType());
    assertEquals("[foreignTableName]", actual.getGraphQLType().toString());
  }
}
