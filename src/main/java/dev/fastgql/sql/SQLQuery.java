/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import static dev.fastgql.sql.SQLUtils.createBoolQuery;
import static dev.fastgql.sql.SQLUtils.createOrderByQuery;

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
  private Set<String> keys;
  private List<String> joins;
  private List<String> suffixes;
  private Map<String, String> tableFieldToAlias;
  private Map<String, Set<String>> aliasToKeys;

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
    this.suffixes = new ArrayList<>();
    this.aliasToKeys = new HashMap<>();
    this.tableFieldToAlias = new HashMap<>();
    this.tableFieldToAlias.put(table, alias);
  }

  public void addKey(String table, String key) {
    if (!aliasToKeys.containsKey(table)) {
      aliasToKeys.put(table, new HashSet<>());
    }
    aliasToKeys.get(table).add(key);
    keys.add(String.format("%s.%s AS %s", table, key, getKeyAlias(table, key)));
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

  public void addTableFieldToAlias(String field, String foreignTableAlias) {
    tableFieldToAlias.put(field, foreignTableAlias);
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
            "SELECT %s FROM %s %s %s %s %s",
            String.join(", ", keys),
            table,
            alias,
            String.join(" ", joins),
            String.join(" ", suffixes),
            buildConstraints())
        .replaceAll("\\s+", " ");
  }

  private String buildConstraints() {
    return String.format(
        "%s %s %s %s",
        buildWhereQuery(), buildOrderByQuery(), buildLimitQuery(), buildOffsetQuery());
  }

  private String buildOrderByQuery() {
    if (args.getOrderBy() == null || !args.getOrderBy().isJsonArray()) {
      return "";
    }
    //    ///////////////
    //    LinkedHashMap<String, String> qualifiedNameToOrder = args.getQualifiedNameToOrderMap();
    //    if (qualifiedNameToOrder.isEmpty()) {
    //      return "";
    //    }
    //    List<String> orderBys =
    //        qualifiedNameToOrder.keySet().stream()
    //            .map(
    //                (qualifiedName) -> {
    //                  QualifiedName qualifiedNameObject = new QualifiedName(qualifiedName);
    //                  String table = qualifiedNameObject.getTableName();
    //                  String key = qualifiedNameObject.getKeyName();
    //                  if (tableFieldToAlias.containsKey(table)
    //                      && aliasToKeys.containsKey(tableFieldToAlias.get(table))
    //                      && aliasToKeys.get(tableFieldToAlias.get(table)).contains(key)) {
    //                    String alias = tableFieldToAlias.get(table);
    //                    String keyAlias = getKeyAlias(alias, key);
    //                    return String.format(
    //                        "%s %s", keyAlias, qualifiedNameToOrder.get(qualifiedName));
    //                  }
    //                  return null;
    //                })
    //            .filter(Objects::nonNull)
    //            .collect(Collectors.toList());
    //    ///////////////////
    return String.format(
        "ORDER BY %s",
        createOrderByQuery(args.getOrderBy().getAsJsonArray(), alias, tableFieldToAlias));
  }

  private String buildLimitQuery() {
    if (args.getLimit() == null) {
      return "";
    }
    return String.format("LIMIT %d", args.getLimit());
  }

  private String buildOffsetQuery() {
    if (args.getOffset() == null) {
      return "";
    }
    return String.format("OFFSET %d", args.getOffset());
  }

  private String buildWhereQuery() {
    if (args.getWhere() == null || !args.getWhere().isJsonObject()) {
      return "";
    }
    return String.format(
        "WHERE %s", createBoolQuery(args.getWhere().getAsJsonObject(), alias, tableFieldToAlias));
  }

  private String getKeyAlias(String table, String key) {
    return String.format("%s_%s", table, key);
  }
}
