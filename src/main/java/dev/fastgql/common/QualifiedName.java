/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.common;

import java.util.Objects;
import org.antlr.v4.runtime.misc.Pair;

/**
 * Class to handle qualified name of a field in a table. It is defined by two elements:
 * parent and child, and has string representation of "parent/child".
 *
 * @author Kamil Bobrowski
 */
public class QualifiedName {
  private final String parent;
  private final String name;
  private final String qualifiedName;

  public static String generate(String parent, String name) {
    QualifiedName tmp = new QualifiedName(parent, name);
    return tmp.getQualifiedName();
  }

  /**
   * Construct qualified name from single string.
   *
   * @param qualifiedName qualified name in a format "parent/name"
   */
  public QualifiedName(String qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    Pair<String, String> splitted = splitName(qualifiedName);
    this.parent = splitted.a;
    this.name = splitted.b;
    this.qualifiedName = qualifiedName;
  }

  /**
   * Construct qualified name from two separate strings for parent and name.
   *
   * @param parent name of a parent
   * @param name name of a child
   */
  public QualifiedName(String parent, String name) {
    Objects.requireNonNull(parent);
    Objects.requireNonNull(name);
    this.parent = parent;
    this.name = name;
    if (parent.isEmpty() || name.isEmpty()) {
      throw new IllegalArgumentException("qualified name: parent or name cannot be empty");
    } else {
      this.qualifiedName = String.format("%s/%s", parent, name);
    }
  }

  public String getName() {
    return name;
  }

  public String getParent() {
    return parent;
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
      throw new IllegalArgumentException(
          "qualified name has to be in the format of \"parent/child\"");
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
    return parent.equals(other.parent)
        && name.equals(other.name)
        && qualifiedName.equals(other.qualifiedName);
  }
}
