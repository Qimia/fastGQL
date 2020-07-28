/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class DatabaseSchemaTest {

  @Test
  public void build_withReferencing() {
    DatabaseSchema databaseSchema =
        DatabaseSchema.newSchema()
            .addKey("tableName/keyName", KeyType.INT, "referencingTableName/referencingKeyName")
            .build();
    KeyDefinition expectedKeyDefinition =
        new KeyDefinition(
            new QualifiedName("tableName/keyName"),
            KeyType.INT,
            new QualifiedName("referencingTableName/referencingKeyName"),
            null);
    KeyDefinition expectedReferencingKeyDefinition =
        new KeyDefinition(
            new QualifiedName("referencingTableName/referencingKeyName"),
            KeyType.INT,
            null,
            Set.of(new QualifiedName("tableName/keyName")));
    assertTrue(
        databaseSchema
            .getTableNames()
            .containsAll(Arrays.asList("tableName", "referencingTableName")));
    assertEquals(2, databaseSchema.getTableNames().size());
    assertEquals(expectedKeyDefinition, databaseSchema.getGraph().get("tableName").get("keyName"));
    assertEquals(
        expectedReferencingKeyDefinition,
        databaseSchema.getGraph().get("referencingTableName").get("referencingKeyName"));
  }

  @Test
  public void build_withoutReferencing() {
    DatabaseSchema databaseSchema =
        DatabaseSchema.newSchema().addKey("tableName/keyName", KeyType.INT).build();
    KeyDefinition expectedKeyDefinition =
        new KeyDefinition(new QualifiedName("tableName/keyName"), KeyType.INT, null, null);
    assertTrue(databaseSchema.getTableNames().contains("tableName"));
    assertEquals(1, databaseSchema.getTableNames().size());
    assertEquals(expectedKeyDefinition, databaseSchema.getGraph().get("tableName").get("keyName"));
  }
}
