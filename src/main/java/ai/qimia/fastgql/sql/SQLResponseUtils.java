package ai.qimia.fastgql.sql;

import ai.qimia.fastgql.graphql.GraphQLDatabaseSchema;
import ai.qimia.fastgql.graphql.GraphQLNodeDefinition;
import graphql.schema.DataFetchingFieldSelectionSet;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.SqlConnection;

import java.util.*;
import java.util.stream.Collectors;

public class SQLResponseUtils {
  public static Object getValue(Map<String, Object> row, String alias, String key) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(alias);
    Objects.requireNonNull(key);
    return row.get(String.format("%s_%s", alias, key));
  }

  public static Single<Map<String, Object>> constructResponse(Map<String, Object> row, List<Component> components) {
    Objects.requireNonNull(row);
    Objects.requireNonNull(components);
    List<Single<Map<String, Object>>> observables = components
      .stream()
      .map(component -> component.extractValues(row))
      .collect(Collectors.toList());
    return Single.zip(observables, values -> {
      Map<String, Object> r = new HashMap<>();
      Arrays
        .stream(values)
        .map(value -> (Map<String, Object>) value)
        .forEach(r::putAll);
      return r;
    });
  }

  public static Single<List<Map<String, Object>>> executeQuery(String query, SqlConnection connection) {
    return connection.rxQuery(query).map(rowSet -> {
      List<String> columnNames = rowSet.columnsNames();
      List<Map<String, Object>> rList = new ArrayList<>();
      rowSet.forEach(row -> {
        Map<String, Object> r = new HashMap<>();
        columnNames.forEach(columnName -> r.put(columnName, row.getValue(columnName)));
        rList.add(r);
      });
      return rList;
    });
  }

  public static void traverseSelectionSet(SqlConnection connection, GraphQLDatabaseSchema graphQLDatabaseSchema, ComponentParent parent, AliasGenerator aliasGenerator, DataFetchingFieldSelectionSet selectionSet) {
    selectionSet.getFields().forEach(field -> {
      // todo: cleaner way to skip non-root nodes?
      if (field.getQualifiedName().contains("/")) {
        return;
      }
      GraphQLNodeDefinition node = graphQLDatabaseSchema.nodeAt(parent.trueTableNameWhenParent(), field.getName());
      switch (node.getReferenceType()) {
        case NONE:
          parent.addComponent(new ComponentRow(node.getQualifiedName().getName()));
          break;
        case REFERENCING:
          Component componentReferencing = new ComponentReferencing(
            field.getName(),
            node.getQualifiedName().getName(),
            node.getForeignName().getParent(),
            aliasGenerator.getAlias(),
            node.getForeignName().getName()
          );
          traverseSelectionSet(connection, graphQLDatabaseSchema, componentReferencing, aliasGenerator, field.getSelectionSet());
          parent.addComponent(componentReferencing);
          break;
        case REFERENCED:
          Component componentReferenced = new ComponentReferenced(
            field.getName(),
            node.getQualifiedName().getName(),
            node.getForeignName().getParent(),
            aliasGenerator.getAlias(),
            node.getForeignName().getName(),
            queryString -> SQLResponseUtils.executeQuery(queryString, connection)
          );
          traverseSelectionSet(connection, graphQLDatabaseSchema, componentReferenced, aliasGenerator, field.getSelectionSet());
          parent.addComponent(componentReferenced);
          break;
        default:
          throw new RuntimeException("Unrecognized reference type");
      }
    });
  }
}
