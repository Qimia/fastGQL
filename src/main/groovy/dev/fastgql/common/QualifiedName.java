/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.common;

import java.util.Objects;
import org.antlr.v4.runtime.misc.Pair;

/**
 * Class to handle qualified name of a key in a table. It is defined by two elements: table and key,
 * and has string representation of "table/key".
 *
 * @author Kamil Bobrowski
 */
public class QualifiedName {
  private final String tableName;
  private final String keyName;
  private final String qualifiedName;

  public static String generate(String tableName, String keyName) {
    QualifiedName tmp = new QualifiedName(tableName, keyName);
    return tmp.getQualifiedName();
  }

  /**
   * Construct qualified name from single string.
   *
   * @param qualifiedName qualified name in a format "table/key"
   */
  public QualifiedName(String qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    Pair<String, String> splitted = splitName(qualifiedName);
    this.tableName = splitted.a;
    this.keyName = splitted.b;
    this.qualifiedName = qualifiedName;
  }

  /**
   * Construct qualified name from two separate strings for table and key.
   *
   * @param tableName name of a parent
   * @param keyName name of a child
   */
  public QualifiedName(String tableName, String keyName) {
    Objects.requireNonNull(tableName);
    Objects.requireNonNull(keyName);
    this.tableName = tableName;
    this.keyName = keyName;
    if (tableName.isEmpty() || keyName.isEmpty()) {
      throw new IllegalArgumentException("qualified name: table or key cannot be empty");
    } else {
      this.qualifiedName = String.format("%s/%s", tableName, keyName);
    }
  }

  public String getKeyName() {
    return keyName;
  }

  public String getTableName() {
    return tableName;
  }

  public String getQualifiedName() {
    return qualifiedName;
  }

  @Override
  public String toString() {
    return qualifiedName;
  }

  private static Pair<String, String> splitName(String name) {
    Pair<String, String> ret;
    String[] splitted = name.split("/");
    if (splitted.length == 2) {
      ret = new Pair<>(splitted[0], splitted[1]);
    } else {
      throw new IllegalArgumentException("qualified name has to be in the format of \"table/key\"");
    }
    return ret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QualifiedName other = (QualifiedName) o;
    return tableName.equals(other.tableName)
        && keyName.equals(other.keyName)
        && qualifiedName.equals(other.qualifiedName);
  }
}
