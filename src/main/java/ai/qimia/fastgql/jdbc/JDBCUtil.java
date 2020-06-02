package ai.qimia.fastgql.jdbc;

import ai.qimia.fastgql.DatasourceConfig;
import ai.qimia.fastgql.schema.DatabaseSchema;
import ai.qimia.fastgql.schema.DatabaseSchema.Builder;
import ai.qimia.fastgql.schema.FieldType;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class JDBCUtil {

  private final static Map<Integer, FieldType> sqlDataTypeToFieldType = Map.of(
      4, FieldType.INT,
      12, FieldType.STRING
  );

  public static DatabaseSchema getDatabaseSchema(Connection connection) throws SQLException {
    DatabaseMetaData databaseMetaData = connection.getMetaData();
    Statement statement = connection.createStatement();
    ResultSet tablesResultSet =
        databaseMetaData.getTables(null, null, null, new String[] {"TABLE"});

    Builder databaseSchemaBuilder = DatabaseSchema.newSchema();

    while (tablesResultSet.next()) {
      String tableName = tablesResultSet.getString("TABLE_NAME");
      System.out.println(tableName);
      // Foreign key extraction
      ResultSet foreignKeyResultSet = databaseMetaData.getImportedKeys(null, null, tableName);
      Map<String, String> foreignKeyToRef = new HashMap<>();
      while (foreignKeyResultSet.next()) {
        String columnName = foreignKeyResultSet.getString("FKCOLUMN_NAME");
        String refColumnName = foreignKeyResultSet.getString("PKCOLUMN_NAME");
        String refTableName = foreignKeyResultSet.getString("PKTABLE_NAME");
        foreignKeyToRef.put(
            String.format("%s/%s", tableName, columnName),
            String.format("%s/%s", refTableName, refColumnName));
      }
      foreignKeyResultSet.close();

      // Column extraction
      ResultSet columnsResultSet = databaseMetaData.getColumns(null, null, tableName, null);
      while (columnsResultSet.next()) {
        Integer dataType = columnsResultSet.getInt("DATA_TYPE");
        if (!sqlDataTypeToFieldType.containsKey(dataType)) {
          throw new RuntimeException("Only integer or string class for columns currently supported");
        }
        String columnName = columnsResultSet.getString("COLUMN_NAME");
        String qualifiedName = String.format("%s/%s", tableName, columnName);
        if (foreignKeyToRef.containsKey(qualifiedName)) {
          databaseSchemaBuilder.row(qualifiedName, sqlDataTypeToFieldType.get(dataType), foreignKeyToRef.get(qualifiedName));
        } else {
          databaseSchemaBuilder.row(qualifiedName, sqlDataTypeToFieldType.get(dataType));
        }
      }
      columnsResultSet.close();
    }
    tablesResultSet.close();
    statement.close();
    connection.close();
    return databaseSchemaBuilder.build();
  }

  public static void main(String[] args) throws SQLException {
    DatasourceConfig datasourceConfig = new DatasourceConfig();
    Connection connection =
        DriverManager.getConnection(
            String.format(
                "jdbc:postgresql://%s:%d/%s",
                datasourceConfig.getHost(), datasourceConfig.getPort(), datasourceConfig.getDb()),
            datasourceConfig.getUsername(),
            datasourceConfig.getPassword());
    DatabaseSchema databaseSchema = getDatabaseSchema(connection);
    System.out.println(databaseSchema);
  }
}
