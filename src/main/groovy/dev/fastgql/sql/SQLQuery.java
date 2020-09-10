/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to construct SQL query from simple query components.
 *
 * @author Kamil Bobrowski
 */
public class SQLQuery {
  private final String table;
  private final String alias;
  private final SQLArguments args;
  private final Set<String> keys;
  private final List<String> joins;
  private final List<String> whereConditions;
  private final Map<String, String> fieldToAlias;

  /**
   * Construct SQLQuery with table and its alias.
   *
   * @param table name of the table
   * @param alias alias of the table
   * @param args arguments parsed from GraphQL query
   */
  public SQLQuery(String table, String alias, SQLArguments args) {
    this.table = table;
    this.alias = alias;
    this.args = args;
    this.keys = new HashSet<>();
    this.joins = new ArrayList<>();
    this.whereConditions = new ArrayList<>();
    this.fieldToAlias = new HashMap<>();
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

  public void addFieldToAlias(String field, String foreignTableAlias) {
    fieldToAlias.put(field, foreignTableAlias);
  }

  public void addWhereConditions(String whereConditional) {
    whereConditions.add(whereConditional);
  }

  /**
   * Build query from internal components.
   *
   * @return valid SQL query to be executed
   */
  public String build() {
    return String.format(
            "SELECT %s FROM %s %s %s %s",
            String.join(", ", keys), table, alias, String.join(" ", joins), buildConstraints())
        .replaceAll("\\s+", " ");
  }

  private String buildConstraints() {
    return String.format(
        "%s %s %s %s",
        buildWhereQuery(), buildOrderByQuery(), buildLimitQuery(), buildOffsetQuery());
  }

  private String buildOrderByQuery() {
    if (args == null || args.getOrderBy() == null) {
      return "";
    }
    return String.format(
        "ORDER BY %s", SQLUtils.createOrderByQuery(args.getOrderBy(), alias, fieldToAlias));
  }

  private String buildLimitQuery() {
    if (args == null || args.getLimit() == null) {
      return "";
    }
    return String.format("LIMIT %d", args.getLimit());
  }

  private String buildOffsetQuery() {
    if (args == null || args.getOffset() == null) {
      return "";
    }
    return String.format("OFFSET %d", args.getOffset());
  }

  private String buildWhereQuery() {
    if (args != null && args.getWhere() != null) {
      String whereQuery = SQLUtils.createBoolQuery(args.getWhere(), alias, fieldToAlias);
      addWhereConditions(String.format("(%s)", whereQuery));
    }
    if (whereConditions.isEmpty()) {
      return "";
    }
    return String.format("WHERE %s", String.join(" AND ", whereConditions));
  }
}
