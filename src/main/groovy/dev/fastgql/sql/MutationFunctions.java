package dev.fastgql.sql;

import dev.fastgql.common.KeyType;
import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.KeyDefinition;
import dev.fastgql.dsl.RoleSpec;
import graphql.language.Argument;
import graphql.language.Field;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MutationFunctions {

  private final DatabaseSchema databaseSchema;
  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;

  public MutationFunctions(DatabaseSchema databaseSchema, RoleSpec roleSpec, Map<String, Object> jwtParams) {
    this.databaseSchema = databaseSchema;
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
  }

  private static String buildMutationQueryFromRow(
    String tableName, Object row, DatabaseSchema databaseSchema) {
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

    return String.format(
      "INSERT INTO %s (%s) VALUES (%s)",
      tableName, String.join(", ", columns), String.join(", ", values));
  }

  public ExecutionDefinition<Map<String, Object>> createExecutionDefinition(Field field, Object rowsObject) {
    String fieldName = field.getName();
    String insertPrefix = "insert_";

    if (!field.getName().startsWith(insertPrefix)) {
      throw new RuntimeException("Field name does not start with insert_");
    }

    String tableName = fieldName.substring(insertPrefix.length());

    Optional<Argument> rowObjectOptional = field.getArguments().stream().filter(argument -> argument.getName().equals("objects"))
      .findFirst();

    if (rowObjectOptional.isEmpty()) {
      throw new RuntimeException("No 'objects' argument in mutation");
    }

    JsonArray rows = new JsonArray((List<?>) rowsObject);

    List<String> queries = rows
      .stream()
      .map(row -> buildMutationQueryFromRow(tableName, row, databaseSchema))
      .collect(Collectors.toList());

    Function<QueryExecutor, Maybe<Map<String, Object>>> queryExecutorFunction =
      queryExecutor -> Observable.fromIterable(queries)
        .flatMapSingle(queryExecutor::apply)
        .reduce(MutationResponse.newMutationResponse(), MutationResponse::compose)
        .map(MutationResponse::build)
        .toMaybe();

    return new ExecutionDefinition<>(queryExecutorFunction, null);
  }
}
