package ai.qimia.fastgql.schema.sql;

import java.util.List;
import java.util.Map;

public interface ComponentExecutable extends ComponentParent {
  void setForgedResponse(List<Map<String, Object>> forgedResponse);
  List<Map<String, Object>> execute();
}
