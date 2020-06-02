package ai.qimia.fastgql.schema.sql;

import io.reactivex.Single;

import java.util.List;
import java.util.Map;

public interface ComponentExecutable extends ComponentParent {
  Single<List<Map<String, Object>>> execute();
}
