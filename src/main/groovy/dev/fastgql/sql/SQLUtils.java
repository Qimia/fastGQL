/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class of utility functions for constructing constraint queries.
 *
 * @author Mingyi Zhang
 */
public class SQLUtils {

  public static List<Map<String, Object>> rowSetToList(RowSet<Row> rowSet) {
    List<String> columnNames = rowSet.columnsNames();
    List<Map<String, Object>> retList = new ArrayList<>();
    rowSet.forEach(
        row -> {
          Map<String, Object> r = new HashMap<>();
          columnNames.forEach(columnName -> r.put(columnName, row.getValue(columnName)));
          retList.add(r);
        });
    return retList;
  }
}
