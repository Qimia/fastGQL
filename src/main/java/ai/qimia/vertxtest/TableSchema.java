package ai.qimia.vertxtest;

import graphql.schema.*;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static graphql.Scalars.*;

public class TableSchema<PKType> {
  @Getter private Map<String, Column<?>> columns = new HashMap<>();
  @Getter private final String name;
  @Getter private final String primaryKeyName;
  private final Map<Class<?>, GraphQLScalarType> classGraphQLScalarTypeMap = Map.of(
    Integer.class, GraphQLInt,
    Long.class, GraphQLInt,
    Float.class, GraphQLFloat,
    Double.class, GraphQLFloat,
    String.class, GraphQLString,
    Boolean.class, GraphQLBoolean
  );
  @Getter private final Class<PKType> primaryKeyClass;

  public TableSchema(final String name, final String primaryKeyName, final Class<PKType> primaryKeyClass) {
    if (!classGraphQLScalarTypeMap.containsKey(primaryKeyClass)) {
      throw new IllegalArgumentException("Primary key class " + primaryKeyClass + " cannot be cast to GraphQL type");
    }
    this.primaryKeyClass = primaryKeyClass;
    this.name = name.toLowerCase();
    this.primaryKeyName = primaryKeyName.toLowerCase();
  }

  public <T> void addColumn(final String name, final Class<T> type) {
    if (!classGraphQLScalarTypeMap.containsKey(type)) {
      throw new IllegalArgumentException("Column class " + type + " cannot be cast to GraphQL type");
    }
    columns.put(name.toLowerCase(), new Column<>(type, null));
  }

  public GraphQLOutputType graphQLOutputType(final String prefix, final Map<String, TableSchema<?>> tableSchemaMap) {
    Set<String> parentNames = new HashSet<>();
    return new GraphQLList(graphQLObjectType(parentNames, prefix, tableSchemaMap));
  }


  private GraphQLObjectType graphQLObjectType(final Set<String> parentNames, final String prefix, final Map<String, TableSchema<?>> tableSchemaMap) {
    Set<String> parentNamesCopy = new HashSet<>(parentNames);
    if (parentNamesCopy.contains(prefix + name)) {
      throw new RuntimeException("circular foreign key relationship between tables");
    }
    parentNamesCopy.add(prefix + name);
    GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
      .name(prefix + name)
      .field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name(primaryKeyName)
          .type(classGraphQLScalarTypeMap.get(primaryKeyClass))
          .build()
      );
    for (Map.Entry<String, Column<?>> entry : columns.entrySet()) {
      Column<?> column = entry.getValue();
      String columnName = entry.getKey();
      if (column.getReferenceTable() != null && !tableSchemaMap.containsKey(column.getReferenceTable())) {
        throw new RuntimeException("non existing table schema referenced");
      }
      GraphQLOutputType graphQLOutputType = column.getReferenceTable() != null
        ? tableSchemaMap.get(column.getReferenceTable()).graphQLObjectType(parentNamesCopy, prefix, tableSchemaMap)
        : classGraphQLScalarTypeMap.get(column.getClazz());
      builder.field(
        GraphQLFieldDefinition.newFieldDefinition()
          .name(columnName)
          .type(graphQLOutputType)
          .build()
      );
    }

    return builder.build();
  }

  @Override
  public String toString() {
    return String.format(
      "%s(%s)",
      name,
      Stream.concat(
        columns.keySet().stream(),
        Stream.of(primaryKeyName)
      )
        .sorted()
        .collect(Collectors.joining(" | "))
    );
  }
}
