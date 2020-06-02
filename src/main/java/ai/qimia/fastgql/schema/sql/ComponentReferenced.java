package ai.qimia.fastgql.schema.sql;

import io.reactivex.Single;

import java.util.Map;

public class ComponentReferenced extends ExecutionRoot implements Component {
  private String table;
  private final String key;
  private final String field;
  private final String foreignTableAlias;
  private final String foreignKey;

  public ComponentReferenced(String field, String key, String foreignTable, String foreignTableAlias, String foreignKey) {
    super(foreignTable, foreignTableAlias);
    this.key = key;
    this.field = field;
    this.foreignTableAlias = foreignTableAlias;
    this.foreignKey = foreignKey;
  }

  @Override
  public void updateQuery(SQLQuery query) {
    query.addKey(String.format("%s.%s AS %s_%s", table, key, table, key));
  }

  @Override
  public void setTable(String table) {
    this.table = table;
  }

  @Override
  public Single<Map<String, Object>> extractValues(Map<String, Object> row) {
    String value = row.get(String.format("%s_%s", table, key)).toString();
    modifyQuery(query -> query.addSuffix(String.format("WHERE %s.%s = %s", foreignTableAlias, foreignKey, value)));
    return execute().map(response -> Map.of(field, response));
  }
}
