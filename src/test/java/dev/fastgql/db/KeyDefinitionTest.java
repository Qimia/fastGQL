/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.db;
import dev.fastgql.common.KeyType;
import dev.fastgql.common.QualifiedName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class KeyDefinitionTest {

    KeyDefinition thisKeyDefinition;

    @BeforeEach
    public void setUp() {
        thisKeyDefinition = new KeyDefinition(
                new QualifiedName("tableName1", "keyName1"),
                KeyType.STRING,
                new QualifiedName("tableName2", "keyName2"),
                new HashSet<>(Arrays.asList(new QualifiedName("tableName3", "keyName3"),
                        new QualifiedName("tableName4", "keyName4"))));
    }

    @Test
    public void shouldMergeEqualKeyDefinitions() {
        Set<QualifiedName> thisReferences = thisKeyDefinition.getReferencedBy();
        thisKeyDefinition.merge(thisKeyDefinition);

        assertTrue(thisReferences.containsAll(thisKeyDefinition.getReferencedBy()));
    }

    @Test
    public void throwsRuntimeExceptionWhileMergingWithNull() {
        Exception exception = assertThrows(RuntimeException.class, () -> {
            thisKeyDefinition.merge(null);
        });

        String expectedMessage = "cannot merge with null key";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void throwsRuntimeExceptionForIncompatibleKeys() {
        KeyDefinition otherKeyDefinition = new KeyDefinition(
                new QualifiedName("tableName1x", "keyName1x"),
                KeyType.STRING,
                new QualifiedName("tableName2x", "keyName2x"),
                new HashSet<>(Arrays.asList(new QualifiedName("tableName6", "keyName6"))));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            thisKeyDefinition.merge(otherKeyDefinition);
        });

        String expectedMessage = "keys to be merged not compatible";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void throwsRuntimeExceptionForDifferentReferencings() {
        KeyDefinition otherKeyDefinition = new KeyDefinition(
                new QualifiedName("tableName1", "keyName1"),
                KeyType.STRING,
                new QualifiedName("tableName2x", "keyName2x"),
                new HashSet<>(Arrays.asList(new QualifiedName("tableName6", "keyName6"))));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            thisKeyDefinition.merge(otherKeyDefinition);
        });

        String expectedMessage = "key is already referencing other key";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void shouldMergeKeyDefinitionsWithDifferentReferences() {
        KeyDefinition otherKeyDefinition = new KeyDefinition(
                new QualifiedName("tableName1", "keyName1"),
                KeyType.STRING,
                new QualifiedName("tableName2", "keyName2"),
                new HashSet<>(Arrays.asList(new QualifiedName("tableName6", "keyName6"))));

        // saving both sets of references in order to generate the expected set
        Set<QualifiedName> thisReferences = thisKeyDefinition.getReferencedBy();
        Set<QualifiedName> otherReferences = otherKeyDefinition.getReferencedBy();
        thisReferences.addAll(otherReferences);

        // merging of KeyDefinitions
        thisKeyDefinition.merge(otherKeyDefinition);

        assertTrue(thisKeyDefinition.getReferencedBy().containsAll(thisReferences));
    }

    @Test
    public void shouldMergeKeyDefinitionsWithNullOtherReferences() {
        KeyDefinition otherKeyDefinition = new KeyDefinition(
                new QualifiedName("tableName1", "keyName1"),
                KeyType.STRING,
                new QualifiedName("tableName2", "keyName2"),
                null);

        // saving the set of 'this' references as the expected set
        Set<QualifiedName> thisReferences = thisKeyDefinition.getReferencedBy();

        // merging of KeyDefinitions
        thisKeyDefinition.merge(otherKeyDefinition);

        assertTrue(thisKeyDefinition.getReferencedBy().containsAll(thisReferences));
    }
}
