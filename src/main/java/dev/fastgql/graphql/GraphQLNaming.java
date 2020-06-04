/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import dev.fastgql.common.QualifiedName;
import java.util.Objects;

public class GraphQLNaming {
  public static String getNameOrderByType(String name) {
    return String.format("%s_order_by", name);
  }

  public static String getNameForReferencingField(QualifiedName qualifiedName) {
    Objects.requireNonNull(qualifiedName);
    return String.format("%s_ref", qualifiedName.getName());
  }

}
