package dev.fastgql.sql;

import dev.fastgql.common.RelationalOperator;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.dsl.*;
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

  static class Pair<L, R> {
    private final L left;
    private final R right;

    Pair(L value, R alias) {
      this.left = value;
      this.right = alias;
    }

    static <L, R> Pair<L, R> of(L left, R right) {
      return new Pair<>(left, right);
    }
  }

  private final RoleSpec roleSpec;
  private final Map<String, Object> jwtParams;

  public MutationFunctions(RoleSpec roleSpec, Map<String, Object> jwtParams) {
    this.roleSpec = roleSpec;
    this.jwtParams = jwtParams;
  }

  private boolean checkCondition(Condition condition, Map<String, Object> columnToValue) {
    String column = condition.getColumn();
    Function<Map<String, Object>, Object> jwtParamsToValue = condition.getFunction();
    boolean valid = true;
    if (jwtParamsToValue != null) {
      Object valueTarget =
          jwtParams != null ? jwtParamsToValue.apply(jwtParams) : jwtParamsToValue.apply(Map.of());
      Object valueCurrent = columnToValue.get(column);
      RelationalOperator relationalOperator = condition.getOperator();

      if (column == null || valueTarget == null) {
        valid = true;
      } else {
        valid =
            condition.isNegated()
                ^ relationalOperator.getValidator().apply(valueCurrent, valueTarget);
      }
    }

    for (Condition nextCondition : condition.getNext()) {
      if (nextCondition.getConnective() == LogicalConnective.or) {
        valid = valid || checkCondition(nextCondition, columnToValue);
      } else {
        valid = valid && checkCondition(nextCondition, columnToValue);
      }
    }
    return valid;
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

    List<Map.Entry<String, Object>> columnNameToValueArguments =
        rowObject.stream()
            .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    List<String> columnsSqlListArguments =
        columnNameToValueArguments.stream()
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableList());

    if (!allowedColumns.containsAll(columnsSqlListArguments)) {
      throw new RuntimeException("Insert query violates server-side permissions");
    }

    List<Preset> presets = opSpec.createPresets();

    List<Map.Entry<String, Object>> columnNameToValuePresets =
        presets == null
            ? List.of()
            : presets.stream()
                .map(preset -> Map.entry(preset.getColumn(), preset.getFunction().apply(jwtParams)))
                .collect(Collectors.toList());

    Map<String, Pair<Object, String>> columnNameToValueAlias =
        Stream.concat(columnNameToValuePresets.stream(), columnNameToValueArguments.stream())
            .collect(
                (Supplier<HashMap<String, Object>>) HashMap::new,
                (accumulator, next) -> accumulator.put(next.getKey(), next.getValue()),
                Map::putAll)
            .entrySet()
            .stream()
            .collect(
                LinkedHashMap::new,
                (accumulator, next) ->
                    accumulator.put(
                        next.getKey(), Pair.of(next.getValue(), placeholderCounter.next())),
                Map::putAll);

    Map<String, Object> columnNameToValue =
        columnNameToValueAlias.entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().left));

    Condition condition =
        opSpec.getCondition() == null ? new Condition(null) : opSpec.getCondition();

    if (!checkCondition(condition, columnNameToValue)) {
      throw new RuntimeException("Insert query violates server-side permissions");
    }

    String columnsSql = String.join(", ", columnNameToValueAlias.keySet());

    String valuesSql =
        columnNameToValueAlias.values().stream()
            .map(valueAlias -> valueAlias.right)
            .collect(Collectors.joining(", "));

    List<Object> params =
        columnNameToValueAlias.values().stream()
            .map(valueAlias -> valueAlias.left)
            .collect(Collectors.toUnmodifiableList());

    return new QueryParams(
        String.format(
            "INSERT INTO %s (%s) VALUES (%s)",
            tableName, String.join(", ", columnsSql), String.join(", ", valuesSql)),
        params);
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
                .flatMapSingle(
                    queryParams -> queryExecutor.apply(queryParams.sql, queryParams.params))
                .reduce(0, (count, rowSet) -> count + rowSet.rowCount())
                .map(value -> Map.of("affected_rows", (Object) value))
                .toMaybe();

    return new ExecutionDefinition<>(queryExecutorFunction, null);
  }
}
