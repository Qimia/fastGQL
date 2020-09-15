package dev.fastgql.sql;

import dev.fastgql.db.DatabaseSchema;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.dsl.OpSpec;
import dev.fastgql.dsl.OpType;
import dev.fastgql.dsl.RoleSpec;
import dev.fastgql.dsl.TableSpec;
import graphql.language.Argument;
import graphql.language.Field;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MutationFunctions {

  static class QueryParams {
    private final String sql;
    private final List<Object> params;

    QueryParams(String sql, List<Object> params) {
      this.sql = sql;
      this.params = params;
    }
  }

  static class ValueAlias {
    private final Object value;
    private final String alias;

    ValueAlias(Object value, String alias) {
      this.value = value;
      this.alias = alias;
    }
  }

  private final DatabaseSchema databaseSchema;
  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;

  public MutationFunctions(
      DatabaseSchema databaseSchema, RoleSpec roleSpec, Map<String, Object> jwtParams) {
    this.databaseSchema = databaseSchema;
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
  }

  private QueryParams buildMutationQueryFromRow(
    String tableName, Object row, DatasourceConfig.DBType dbType) {
    JsonObject rowObject = JsonObject.mapFrom(row);

    PlaceholderCounter placeholderCounter = new PlaceholderCounter(dbType);

    TableSpec tableSpec = roleSpec.getTable(tableName);
    if (tableSpec == null) {
      throw new RuntimeException("No permissions defined for table table " + tableName);
    }

    OpSpec opSpec = tableSpec.getOp(OpType.insert);
    if (opSpec == null) {
      throw new RuntimeException("User does not have insert permissions on table " + tableName);
    }

    List<String> allowedColumns = opSpec.getAllowed();

    List<Map.Entry<String, Object>> columnNameToValueArguments = rowObject.stream()
      .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
      .collect(Collectors.toList());

    List<String> columnsSqlListArguments = columnNameToValueArguments.stream()
      .map(Map.Entry::getKey)
      .collect(Collectors.toUnmodifiableList());

    if (!allowedColumns.containsAll(columnsSqlListArguments)) {
      throw new RuntimeException("User does not have permissions to insert into table " + tableName);
    }

    List<Preset> presets = opSpec.createPresets();

    List<Map.Entry<String, Object>> columnNameToValuePresets = presets == null ? List.of() : presets.stream()
      .map(preset -> Map.entry(preset.getColumn(), preset.getFunction().apply(jwtParams)))
      .collect(Collectors.toList());

    Map<String, ValueAlias> columnNameToValueAlias = Stream.concat(
      columnNameToValuePresets.stream(), columnNameToValueArguments.stream()
    ).collect(
      (Supplier<HashMap<String, Object>>) HashMap::new,
      (accumulator, next) -> accumulator.put(next.getKey(), next.getValue()),
      Map::putAll
    ).entrySet().stream().collect(
      LinkedHashMap::new,
      (accumulator, next) -> accumulator.put(next.getKey(), new ValueAlias(next.getValue(), placeholderCounter.next())),
      Map::putAll
    );

    String columnsSql = String.join(", ", columnNameToValueAlias.keySet());

    String valuesSql = columnNameToValueAlias.values().stream()
      .map(valueAlias -> valueAlias.alias)
      .collect(Collectors.joining(", "));

    List<Object> params = columnNameToValueAlias.values().stream()
      .map(valueAlias -> valueAlias.value)
      .collect(Collectors.toUnmodifiableList());

    return new QueryParams(String.format(
        "INSERT INTO %s (%s) VALUES (%s)",
        tableName, String.join(", ", columnsSql), String.join(", ", valuesSql)), params);
  }

  public ExecutionDefinition<Map<String, Object>> createExecutionDefinition(
    Field field, Object rowsObject, DatasourceConfig.DBType dbType) {
    String fieldName = field.getName();
    String insertPrefix = "insert_";

    if (!field.getName().startsWith(insertPrefix)) {
      throw new RuntimeException("Field name does not start with insert_");
    }

    String tableName = fieldName.substring(insertPrefix.length());

    Optional<Argument> rowObjectOptional =
        field.getArguments().stream()
            .filter(argument -> argument.getName().equals("objects"))
            .findFirst();

    if (rowObjectOptional.isEmpty()) {
      throw new RuntimeException("No 'objects' argument in mutation");
    }

    JsonArray rows = new JsonArray((List<?>) rowsObject);

    List<QueryParams> queries =
        rows.stream()
            .map(row -> buildMutationQueryFromRow(tableName, row, dbType))
            .collect(Collectors.toList());

    Function<QueryExecutor, Maybe<Map<String, Object>>> queryExecutorFunction =
        queryExecutor ->
            Observable.fromIterable(queries)
                .flatMapSingle(queryParams -> queryExecutor.apply(queryParams.sql, queryParams.params))
                .reduce(0, (count, rowSet) -> count + rowSet.rowCount())
                .map(value -> Map.of("affected_rows", (Object) value))
                .toMaybe();

    return new ExecutionDefinition<>(queryExecutorFunction, null);
  }
}
