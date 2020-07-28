/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static graphql.Scalars.GraphQLInt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import dev.fastgql.common.ReferenceType;
import org.junit.jupiter.api.Test;

public class GraphQLFieldTest {

  @Test
  public void createLeaf() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    GraphQLField actual = GraphQLField.createLeaf(qualifiedName, KeyType.INT);
    assertEquals(qualifiedName, actual.getQualifiedName());
    assertEquals(GraphQLInt, actual.getGraphQLType());
    assertNull(actual.getForeignName());
    assertEquals(ReferenceType.NONE, actual.getReferenceType());
  }

  @Test
  public void createReferencing() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    QualifiedName foreignName = new QualifiedName("foreignTableName/foreignKeyName");
    GraphQLField actual = GraphQLField.createReferencing(qualifiedName, foreignName);
    assertEquals(qualifiedName, actual.getQualifiedName());
    assertEquals(foreignName, actual.getForeignName());
    assertEquals(ReferenceType.REFERENCING, actual.getReferenceType());
    assertEquals("foreignTableName", actual.getGraphQLType().getName());
  }

  @Test
  public void createReferencedBy() {
    QualifiedName qualifiedName = new QualifiedName("tableName/keyName");
    QualifiedName foreignName = new QualifiedName("foreignTableName/foreignKeyName");
    GraphQLField actual = GraphQLField.createReferencedBy(qualifiedName, foreignName);
    assertEquals(qualifiedName, actual.getQualifiedName());
    assertEquals(foreignName, actual.getForeignName());
    assertEquals(ReferenceType.REFERENCED, actual.getReferenceType());
    assertEquals("[foreignTableName]", actual.getGraphQLType().toString());
  }
}
