package ai.qimia.fastgql.schema.sql;

import io.reactivex.Single;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ComponentReferenced extends ExecutionRoot implements Component {
  private String table;
  private final String key;
  private final String field;
  private final String foreignTableAlias;
  private final String foreignKey;

  public ComponentReferenced(String field,
                             String key,
                             String foreignTable,
                             String foreignTableAlias,
                             String foreignKey,
                             Function<String, Single<List<Map<String, Object>>>> sqlExecutor) {
    super(foreignTable, foreignTableAlias, sqlExecutor);
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
    Object value = SQLResponseProcessor.getValue(row, table, key);
    if (value == null) {
      return Single.just(Map.of());
    }
    modifyQuery(query -> query.addSuffix(String.format("WHERE %s.%s = %s", foreignTableAlias, foreignKey, value.toString())));
    return execute().map(response -> Map.of(field, response));
  }
}
