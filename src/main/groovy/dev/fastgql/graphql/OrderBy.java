/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.graphql;

import graphql.schema.GraphQLEnumType;

public class OrderBy {

  public enum Enum {
    asc("asc", "ASC", "in the ascending order, nulls last"),
    asc_nulls_first("asc_nulls_first", "ASC NULLS FIRST", "in the ascending order, nulls first"),
    asc_nulls_last("asc_nulls_last", "ASC NULLS LAST", "in the ascending order, nulls last"),
    desc("desc", "DESC", "in the descending order, nulls last"),
    desc_nulls_first(
        "desc_nulls_first", "DESC NULLS FIRST", "in the descending order, nulls first"),
    desc_nulls_last("desc_nulls_last", "DESC NULLS LAST", "in the descending order, nulls last");

    public final String name;
    public final String value;
    public final String description;

    Enum(String name, String value, String description) {
      this.name = name;
      this.value = value;
      this.description = description;
    }
  }

  public static final GraphQLEnumType enumType =
      GraphQLEnumType.newEnum()
          .name("order_by")
          .description("column ordering options")
          .value(Enum.asc.name, Enum.asc.value, Enum.asc.description)
          .value(
              Enum.asc_nulls_first.name,
              Enum.asc_nulls_first.value,
              Enum.asc_nulls_first.description)
          .value(
              Enum.asc_nulls_last.name, Enum.asc_nulls_last.value, Enum.asc_nulls_last.description)
          .value(Enum.desc.name, Enum.desc.value, Enum.desc.description)
          .value(
              Enum.desc_nulls_first.name,
              Enum.desc_nulls_first.value,
              Enum.desc_nulls_first.description)
          .value(
              Enum.desc_nulls_last.name,
              Enum.desc_nulls_last.value,
              Enum.desc_nulls_last.description)
          .build();
}
