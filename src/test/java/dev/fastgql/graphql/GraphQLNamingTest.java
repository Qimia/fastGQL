/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.fastgql.common.QualifiedName;
import org.junit.jupiter.api.Test;

public class GraphQLNamingTest {

  @Test
  public void getNameOrderByType() {
    assertEquals("name_order_by", GraphQLNaming.getNameOrderByType("name"));
  }

  @Test
  public void getNameBoolType() {
    assertEquals("name_bool_exp", GraphQLNaming.getNameBoolType("name"));
  }

  @Test
  public void getNameForReferencingField() {
    String actualName =
        GraphQLNaming.getNameForReferencingField(new QualifiedName("tableName/keyName"));
    assertEquals("keyName_ref", actualName);
  }

  @Test
  public void getNameForReferencedByField() {
    String actualName =
        GraphQLNaming.getNameForReferencedByField(new QualifiedName("tableName/keyName"));
    assertEquals("tableName_on_keyName", actualName);
  }
}
