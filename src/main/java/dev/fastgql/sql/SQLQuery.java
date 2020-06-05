/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class to construct SQL query from simple query components.
 *
 * @author Kamil Bobrowski
 */
public class SQLQuery {
  private final String table;
  private final String alias;
  private Set<String> keys;
  private List<String> joins;
  private List<String> suffixes;

  /**
   * Construct SQLQuery with table and its alias.
   *
   * @param table name of the table
   * @param alias alias of the table
   */
  public SQLQuery(String table, String alias) {
    this.table = table;
    this.alias = alias;
    this.keys = new HashSet<>();
    this.joins = new ArrayList<>();
    this.suffixes = new ArrayList<>();
  }

  public void addKey(String table, String key) {
    keys.add(String.format("%s.%s AS %s_%s", table, key, table, key));
  }

  /**
   * Add LEFT JOIN statement to SQL query.
   *
   * @param thisTable name of base table
   * @param thisKey name of a key in base table on which the join will be made
   * @param foreignTable name of a table to join
   * @param foreignTableAlias alias of a table to join
   * @param foreignKey name of a key in a table being joined on which join will be made
   */
  public void addJoin(
      String thisTable,
      String thisKey,
      String foreignTable,
      String foreignTableAlias,
      String foreignKey) {
    joins.add(
        String.format(
            "LEFT JOIN %s %s ON %s.%s = %s.%s",
            foreignTable, foreignTableAlias, thisTable, thisKey, foreignTableAlias, foreignKey));
  }

  public void addSuffix(String suffix) {
    suffixes.add(suffix);
  }

  /** Reset query to initial state. */
  public void reset() {
    keys = new HashSet<>();
    joins = new ArrayList<>();
    suffixes = new ArrayList<>();
  }

  /**
   * Build query from internal components.
   *
   * @return valid SQL query to be executed
   */
  public String build() {
    return String.format(
        "SELECT %s FROM %s %s %s %s",
        String.join(", ", keys), table, alias, String.join(" ", joins), String.join(" ", suffixes));
  }
}
