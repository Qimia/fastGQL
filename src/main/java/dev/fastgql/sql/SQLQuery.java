/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.common.QualifiedName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SQLQuery {
  private final String table;
  private final String alias;
  private final SQLArguments args;
  private Set<String> keys;
  private List<String> joins;
  private List<String> suffixes;
  private Map<String, String> tableFieldToAlias;
  private Map<String, Set<String>> aliasToKeys;

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

  public void reset() {
    keys = new HashSet<>();
    joins = new ArrayList<>();
    suffixes = new ArrayList<>();
  }

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
    return String.format("%s %s %s", buildOrderByQuery(), buildLimitQuery(), buildOffsetQuery());
  }

  private String buildOrderByQuery() {
    LinkedHashMap<String, String> qualifiedNameToOrder = args.getQualifiedNameToOrderMap();
    if (qualifiedNameToOrder.isEmpty()) {
      return "";
    }
    List<String> orderBys =
        qualifiedNameToOrder.keySet().stream()
            .map(
                (qualifiedName) -> {
                  QualifiedName qualifiedNameObject = new QualifiedName(qualifiedName);
                  String table = qualifiedNameObject.getParent();
                  String key = qualifiedNameObject.getName();
                  if (tableFieldToAlias.containsKey(table)
                      && aliasToKeys.containsKey(tableFieldToAlias.get(table))
                      && aliasToKeys.get(tableFieldToAlias.get(table)).contains(key)) {
                    String alias = tableFieldToAlias.get(table);
                    String keyAlias = getKeyAlias(alias, key);
                    return String.format(
                        "%s %s", keyAlias, qualifiedNameToOrder.get(qualifiedName));
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    return String.format("ORDER BY %s", String.join(", ", orderBys));
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

  private String getKeyAlias(String table, String key) {
    return String.format("%s_%s", table, key);
  }
}
