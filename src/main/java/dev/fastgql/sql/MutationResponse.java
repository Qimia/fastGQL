package dev.fastgql.sql;

import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MutationResponse {
  private final int affectedRows;
  private final List<Map<String, Object>> returning;

  private MutationResponse() {
    affectedRows = 0;
    returning = List.of();
  }

  private MutationResponse(int affectedRows, List<Map<String, Object>> returning) {
    this.affectedRows = affectedRows;
    this.returning = returning;
  }

  public static MutationResponse newMutationResponse() {
    return new MutationResponse();
  }

  public static MutationResponse compose(MutationResponse other, RowSet<Row> rowSet) {
    List<Map<String, Object>> rows = SQLUtils.rowSetToList(rowSet);
    return new MutationResponse(
        other.affectedRows + rowSet.rowCount(),
        Stream.concat(other.returning.stream(), rows.stream()).collect(Collectors.toList()));
  }

  public Map<String, Object> build() {
    return Map.of("affected_rows", affectedRows, "returning", returning);
  }
}
