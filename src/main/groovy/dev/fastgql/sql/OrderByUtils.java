package dev.fastgql.sql;

import dev.fastgql.graphql.GraphQLDatabaseSchema;
import dev.fastgql.graphql.GraphQLField;
import graphql.language.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderByUtils {

  public static String orderByToSQL(List<OrderBy> orderByList) {
    System.out.println(orderByList);
    return orderByList.stream().map(OrderBy::sqlString).collect(Collectors.joining(", "));
  }

  public static Set<TableAlias> orderByListToTableAliasSet(List<OrderBy> orderByList) {
    return orderByList.stream()
        .map(OrderBy::createTableAliasSet)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static Stream<OrderBy> createOrderByStream(
      ObjectValue objectValue,
      String tableName,
      String tableAlias,
      String foreignTableAlias,
      List<LeftJoin> leftJoins,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    return objectValue.getChildren().stream()
        .map(node -> (ObjectField) node)
        .flatMap(
            objectField -> {
              GraphQLField graphQLField =
                  graphQLDatabaseSchema.fieldAt(tableName, objectField.getName());
              switch (graphQLField.getReferenceType()) {
                case REFERENCING:
                  List<LeftJoin> leftJoinsCopy = new ArrayList<>(leftJoins);
                  String foreignTableName = graphQLField.getForeignName().getTableName();
                  String nextForeignAlias = foreignTableAlias + "r";
                  leftJoinsCopy.add(
                      new LeftJoin(
                          foreignTableName,
                          foreignTableAlias,
                          tableAlias,
                          graphQLField.getQualifiedName().getKeyName(),
                          graphQLField.getForeignName().getKeyName()));
                  return createOrderByStream(
                      (ObjectValue) objectField.getValue(),
                      foreignTableName,
                      foreignTableAlias,
                      nextForeignAlias,
                      leftJoinsCopy,
                      graphQLDatabaseSchema);
                case NONE:
                  return Stream.of(
                      new OrderBy(
                          leftJoins,
                          new SelectColumn(tableAlias, objectField.getName()),
                          ((EnumValue) objectField.getValue()).getName()));
                default:
                  throw new RuntimeException("Unsupported field type for order by");
              }
            });
  }

  static List<OrderBy> createOrderBy(
      Argument argument,
      String tableName,
      String tableAlias,
      GraphQLDatabaseSchema graphQLDatabaseSchema) {
    Stream<OrderBy> orderByStream =
        argument.getValue() instanceof ArrayValue
            ? ((ArrayValue) argument.getValue())
                .getValues().stream()
                    .flatMap(
                        value ->
                            createOrderByStream(
                                (ObjectValue) value,
                                tableName,
                                tableAlias,
                                "f",
                                List.of(),
                                graphQLDatabaseSchema))
            : createOrderByStream(
                (ObjectValue) argument.getValue(),
                tableName,
                tableAlias,
                "f",
                List.of(),
                graphQLDatabaseSchema);
    return orderByStream.collect(Collectors.toList());
  }
}
