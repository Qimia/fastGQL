package ai.qimia.fastgql.schema.sql;

import java.util.ArrayList;
import java.util.List;

public class SQLQuery {
  private final String table;
  private final String alias;
  private List<String> keys;
  private List<String> joins;
  private List<String> suffixes;

  public SQLQuery(String table, String alias) {
    this.table = table;
    this.alias = alias;
    this.keys = new ArrayList<>();
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
    keys = new ArrayList<>();
    joins = new ArrayList<>();
    suffixes = new ArrayList<>();
  }

  public String build() {
    return String.format("SELECT %s FROM %s %s %s %s", String.join(", ", keys), table, alias, String.join(" ", joins), String.join(" ", suffixes));
  }
}
