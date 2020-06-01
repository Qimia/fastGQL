package ai.qimia.fastgql.schema.sql;

import java.util.Map;

public interface Component {
  void updateQuery(SQLQuery query);
  void setTable(String table);
  Map<String, Object> extractValues(Map<String, Object> row);
}
