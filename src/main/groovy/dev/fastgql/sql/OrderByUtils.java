package dev.fastgql.sql;

import graphql.language.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OrderByUtils {
  public static String orderByToSQL(
      List<OrderBy> orderByList, Map<String, String> pathInQueryToAlias) {
    return orderByList.stream()
        .map(
            orderBy ->
                String.format(
                    "%s.%s %s",
                    pathInQueryToAlias.get(orderBy.getPathInQuery()),
                    orderBy.getColumn(),
                    orderBy.getOrder()))
        .collect(Collectors.joining(", "));
  }

  private static Stream<OrderBy> createOrderByStream(ObjectValue objectValue, String pathInQuery) {
    return objectValue.getChildren().stream()
        .map(node -> (ObjectField) node)
        .flatMap(
            objectField ->
                objectField.getValue() instanceof ObjectValue
                    ? createOrderByStream(
                        (ObjectValue) objectField.getValue(),
                        String.format("%s/%s", pathInQuery, objectField.getName()))
                    : Stream.of(
                        new OrderBy(
                            pathInQuery,
                            objectField.getName(),
                            ((EnumValue) objectField.getValue()).getName())));
  }

  static List<OrderBy> createOrderBy(Argument argument, String tableName) {
    Stream<OrderBy> orderByStream =
        argument.getValue() instanceof ArrayValue
            ? ((ArrayValue) argument.getValue())
                .getValues().stream()
                    .flatMap(value -> createOrderByStream((ObjectValue) value, tableName))
            : createOrderByStream((ObjectValue) argument.getValue(), tableName);
    return orderByStream.collect(Collectors.toList());
  }
}
