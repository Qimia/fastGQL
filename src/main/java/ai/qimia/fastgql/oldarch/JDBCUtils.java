/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package ai.qimia.fastgql.oldarch;

import ai.qimia.fastgql.oldarch.arguments.ConditionalOperatorHelpers;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JDBCUtils {

  private final static Gson gson = new Gson();

  private final static Map<Integer, Class<?>> sqlTypeToClass = Map.of(
      4, Integer.class,
      12, String.class
  );

  public static Single<List<Map<String, Object>>> getGraphQLResponse(
      DataFetchingEnvironment environment, Pool client) {
    String tableName = environment.getField().getName();

    Map<String, Object> args = environment.getArguments();
    Integer limit = (Integer) args.get("limit");
    Integer offset = (Integer) args.get("offset");
    JsonElement orderBy = gson.toJsonTree(args.get("order_by"));
    Object distinctOn = args.get("distinct_on");
    JsonElement where = gson.toJsonTree(args.get("where"));

    String query = "SELECT ";

    // DISTINCT ON
    if (distinctOn != null) {
      query += String.format(
          "DISTINCT ON ( %s ) ",
          String.join(", ", (List<String>) distinctOn));
    }

    // SELECT FROM
    List<String> fieldsToQuery = environment
        .getSelectionSet()
        .getFields()
        .stream()
        .map(SelectedField::getName)
        .collect(Collectors.toList());
    String fieldsToQueryComaSeparated = String.join(", ", fieldsToQuery);
    query += String.format("%s FROM %s ", fieldsToQueryComaSeparated, tableName);

    // WHERE
    if (where.isJsonObject()) {
      query += String
          .format("WHERE %s ",
              ConditionalOperatorHelpers.getConditionQuery(where.getAsJsonObject()));
    }

    // LIMIT
    if (limit != null) {
      query += String.format("LIMIT %d ", limit);
    }

    // OFFSET
    if (offset != null) {
      query += String.format("OFFSET %d ", offset);
    }

    // ORDER BY
    // TODO: sorting based on nested object's fields
    if (orderBy.isJsonArray()) {
      query += "ORDER BY ";
      List<String> orderQueryList = new ArrayList<>(List.of());
      JsonArray orderByArray = orderBy.getAsJsonArray();
      for (int i = 0; i < orderByArray.size(); i++) {
        JsonObject object = orderByArray.get(i).getAsJsonObject();
        for (String key : object.keySet()) {
          orderQueryList.add(String.format("%s %s ", key, object.get(key).getAsString()));
        }
      }
      query += String.join(", ", orderQueryList);
    }

    Single<RowSet<Row>> rowSet = client.rxQuery(query);
    return rowSet.map(
        rows -> {
          List<Map<String, Object>> rowMaps = new ArrayList<>(rows.size());
          for (Row row : rows) {
            Map<String, Object> rowMap = new HashMap<>();
            for (String field : fieldsToQuery) {
              rowMap.put(field, row.getValue(field));
            }
            rowMaps.add(rowMap);
          }
          return rowMaps;
        }
    );
  }

  public static TableSchema<?> getTableSchema(DatabaseMetaData databaseMetaData,
      Statement statement, String tableName) throws SQLException {
    ResultSet primaryKeysResultSet = databaseMetaData.getPrimaryKeys(null, null, tableName);
    String primaryKeyName = null;
    while (primaryKeysResultSet.next()) {
      if (primaryKeyName != null) {
        throw new RuntimeException("Only 1 primary key is currently supported");
      }
      primaryKeyName = primaryKeysResultSet.getString("COLUMN_NAME");
    }
    if (primaryKeyName == null) {
      throw new RuntimeException("Primary key not found");
    }
    primaryKeysResultSet.close();
    ResultSet rowsResultSet = statement
        .executeQuery(String.format("SELECT * FROM %s LIMIT 0", tableName));
    ResultSetMetaData rowsResultSetMetaData = rowsResultSet.getMetaData();
    int primaryKeyColumnNumber = rowsResultSet.findColumn(primaryKeyName);
    int primaryKeyType = rowsResultSetMetaData.getColumnType(primaryKeyColumnNumber);
    int columnCount = rowsResultSetMetaData.getColumnCount();
    if (!sqlTypeToClass.containsKey(primaryKeyType)) {
      throw new RuntimeException("Only integer or string class for columns currently supported");
    }
    TableSchema<?> table = new TableSchema<>(tableName, primaryKeyName,
        sqlTypeToClass.get(primaryKeyType));

    for (int i = 1; i <= columnCount; i++) {
      if (i == primaryKeyColumnNumber) {
        continue;
      }
      String columnName = rowsResultSetMetaData.getColumnName(i);
      int columnType = rowsResultSetMetaData.getColumnType(i);
      if (!sqlTypeToClass.containsKey(columnType)) {
        throw new RuntimeException("Only integer or string class for columns currently supported");
      }
      table.addColumn(columnName, sqlTypeToClass.get(columnType));
    }
    rowsResultSet.close();
    return table;
  }

  public static Map<String, TableSchema<?>> getTableSchemas(Connection connection)
      throws SQLException {
    DatabaseMetaData databaseMetaData = connection.getMetaData();
    Statement statement = connection.createStatement();
    ResultSet getTablesResultSet = databaseMetaData
        .getTables(null, null, null, new String[]{"TABLE"});
    Map<String, TableSchema<?>> tables = new HashMap<>();
    while (getTablesResultSet.next()) {
      String tableName = getTablesResultSet.getString("TABLE_NAME");
      tables.put(tableName, getTableSchema(databaseMetaData, statement, tableName));
    }
    getTablesResultSet.close();
    statement.close();
    connection.close();
    return tables;
  }

}
