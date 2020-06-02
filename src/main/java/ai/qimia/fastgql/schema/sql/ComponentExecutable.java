package ai.qimia.fastgql.schema.sql;

import io.reactivex.Single;

import java.util.List;
import java.util.Map;

public interface ComponentExecutable extends ComponentParent {
  void setForgedResponse(Single<List<Map<String, Object>>> forgedResponse);
  Single<List<Map<String, Object>>> execute();
}
