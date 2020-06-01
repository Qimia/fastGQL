package ai.qimia.fastgql.schema.sql;

import java.util.Map;

public class ComponentReferenced extends ExecutionRoot implements Component {
  private final String table;
  private final String key;
  private final String foreignTable;
  private final String foreignTableAlias;
  private final String foreignKey;

  public ComponentReferenced(String field, String table, String key, String foreignTable, String foreignTableAlias, String foreignKey) {
    super(foreignTable, foreignTableAlias);
    this.table = table;
    this.key = key;
    this.foreignTable = foreignTable;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKey = foreignKey;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    query.addKey(String.format("%s.%s", table, key));
  }

  @Override
  public Map<String, Object> extractValues(Map<String, Object> row) {
    String value = row.get(String.format("%s_%s", table, key)).toString();
    getQuery().addSuffix(String.format("WHERE %s.%s = %s", foreignTableAlias, foreignKey, value));
    return execute();
  }
}
