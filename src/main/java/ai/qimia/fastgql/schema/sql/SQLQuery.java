package ai.qimia.fastgql.schema.sql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SQLQuery {
  private final String table;
  private final String alias;
  private Set<String> keys;
  private List<String> joins;
  private List<String> suffixes;

  public SQLQuery(String table, String alias) {
    this.table = table;
    this.alias = alias;
    this.keys = new HashSet<>();
    this.joins = new ArrayList<>();
    this.suffixes = new ArrayList<>();
  }

  public void addKey(String key) {
    keys.add(key);
  }

  public void addJoin(String thisTable, String thisKey, String foreignTable, String foreignTableAlias, String foreignKey) {
    joins.add(String.format("LEFT JOIN %s %s ON %s.%s = %s.%s", foreignTable, foreignTableAlias, thisTable, thisKey, foreignTableAlias, foreignKey));
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
    return String.format("SELECT %s FROM %s %s %s %s", String.join(", ", keys), table, alias, String.join(" ", joins), String.join(" ", suffixes));
  }
}
