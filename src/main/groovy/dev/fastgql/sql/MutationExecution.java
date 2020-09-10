package dev.fastgql.sql;

import dev.fastgql.common.KeyType;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.KeyDefinition;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.sqlclient.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MutationExecution {

  public static Single<Map<String, Object>> createResponse(
      Transaction transaction,
      DatabaseSchema databaseSchema,
      String fieldName,
      JsonArray rows,
      List<String> returningColumns) {
    String insertPrefix = "insert_";
    if (fieldName.startsWith(insertPrefix) && rows != null) {
      String tableName = fieldName.substring(insertPrefix.length());
      List<String> queries = new ArrayList<>();
      rows.forEach(
          row ->
              queries.add(
                  buildMutationQueryFromRow(tableName, row, returningColumns, databaseSchema)));
      Single<MutationResponse> mutationResponse =
          Flowable.fromIterable(queries)
              .flatMap(query -> transaction.rxQuery(query).toFlowable())
              .reduce(MutationResponse.newMutationResponse(), MutationResponse::compose);
      return mutationResponse.map(MutationResponse::build);
    }
    return Single.just(Map.of());
  }

  private static String buildMutationQueryFromRow(
      String tableName, Object row, List<String> returningColumns, DatabaseSchema databaseSchema) {
    JsonObject rowObject = JsonObject.mapFrom(row);
    List<String> columns = new ArrayList<>();
    List<String> values = new ArrayList<>();
    rowObject.forEach(
        entry -> {
          KeyDefinition keyDefinition =
              databaseSchema.getGraph().get(tableName).get(entry.getKey());
          String value;
          if (keyDefinition.getKeyType().equals(KeyType.STRING)) {
            value = String.format("'%s'", entry.getValue());
          } else {
            value = entry.getValue().toString();
          }
          columns.add(entry.getKey());
          values.add(value);
        });
    String returnQuery =
        String.format(
            "INSERT INTO %s (%s) VALUES (%s)",
            tableName, String.join(", ", columns), String.join(", ", values));

    if (returningColumns.size() > 0) {
      returnQuery =
          String.format("%s RETURNING %s", returnQuery, String.join(", ", returningColumns));
    }

    return returnQuery;
  }
}
