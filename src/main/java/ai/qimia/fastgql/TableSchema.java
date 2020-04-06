/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package ai.qimia.fastgql;

import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLString;

import ai.qimia.fastgql.arguments.ConditionalOperatorTypes;
import ai.qimia.fastgql.arguments.OrderBy;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLTypeReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;

public class TableSchema<PKType> {

  @Getter
  private Map<String, Column<?>> columns = new HashMap<>();
  @Getter
  private final String name;
  @Getter
  private final String primaryKeyName;
  private final Map<Class<?>, GraphQLScalarType> classGraphQLScalarTypeMap = Map.of(
      Integer.class, GraphQLInt,
      Long.class, GraphQLInt,
      Float.class, GraphQLFloat,
      Double.class, GraphQLFloat,
      String.class, GraphQLString,
      Boolean.class, GraphQLBoolean
  );
  @Getter
  private final Class<PKType> primaryKeyClass;

  public TableSchema(final String name, final String primaryKeyName,
      final Class<PKType> primaryKeyClass) {
    if (!classGraphQLScalarTypeMap.containsKey(primaryKeyClass)) {
      throw new IllegalArgumentException(
          "Primary key class " + primaryKeyClass + " cannot be cast to GraphQL type");
    }
    this.primaryKeyClass = primaryKeyClass;
    this.name = name.toLowerCase();
    this.primaryKeyName = primaryKeyName.toLowerCase();
  }

  public <T> void addColumn(final String name, final Class<T> type) {
    if (!classGraphQLScalarTypeMap.containsKey(type)) {
      throw new IllegalArgumentException(
          "Column class " + type + " cannot be cast to GraphQL type");
    }
    columns.put(name.toLowerCase(), new Column<>(type, null));
  }

  public GraphQLOutputType graphQLOutputType(final Map<String, TableSchema<?>> tableSchemaMap) {
    Set<String> parentNames = new HashSet<>();
    return new GraphQLList(graphQLObjectType(parentNames, tableSchemaMap));
  }

  private GraphQLObjectType graphQLObjectType(final Set<String> parentNames,
      final Map<String, TableSchema<?>> tableSchemaMap) {
    Set<String> parentNamesCopy = new HashSet<>(parentNames);
    if (parentNamesCopy.contains(name)) {
      throw new RuntimeException("circular foreign key relationship between tables");
    }
    parentNamesCopy.add(name);
    GraphQLObjectType.Builder builder = GraphQLObjectType.newObject()
        .name(name)
        .field(
            GraphQLFieldDefinition.newFieldDefinition()
                .name(primaryKeyName)
                .type(classGraphQLScalarTypeMap.get(primaryKeyClass))
                .build()
        );
    for (Map.Entry<String, Column<?>> entry : columns.entrySet()) {
      Column<?> column = entry.getValue();
      String columnName = entry.getKey();
      if (column.getReferenceTable() != null && !tableSchemaMap
          .containsKey(column.getReferenceTable())) {
        throw new RuntimeException("non existing table schema referenced");
      }
      GraphQLOutputType graphQLOutputType = column.getReferenceTable() != null
          ? tableSchemaMap.get(column.getReferenceTable())
          .graphQLObjectType(parentNamesCopy, tableSchemaMap)
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

  // TODO: sorting based on nested object's fields
  // https://hasura.io/docs/1.0/graphql/manual/queries/sorting.html#sorting-based-on-nested-object-s-fields
  public GraphQLInputType orderByType() {
    GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
    builder.name(name + "_order_by")
        .description("ordering options when selecting data from \"" + name + "\"")
        .field(GraphQLInputObjectField.newInputObjectField()
            .name(primaryKeyName)
            .type(OrderBy.enumType)
            .build());
    columns.keySet().forEach(key -> builder.field(GraphQLInputObjectField.newInputObjectField()
        .name(key)
        .type(OrderBy.enumType)
        .build()));
    return GraphQLList.list(builder.build());
  }

  public GraphQLInputType selectColumnType() {
    GraphQLEnumType.Builder builder = GraphQLEnumType.newEnum();
    builder.name(name + "_select_column")
        .description("select columns of table \"" + name + "\"")
        .value(primaryKeyName, primaryKeyName, "Primary key column name");
    columns.keySet().forEach(key -> builder.value(key, key, "Column name"));
    return GraphQLList.list(builder.build());
  }

  // TODO: filter based on nested objects' fields
  // https://hasura.io/docs/1.0/graphql/manual/queries/query-filters.html#filter-based-on-nested-objects-fields
  public GraphQLInputObjectType boolExpType() {
    String typeName = name + "_bool_exp";
    GraphQLInputObjectType.Builder builder = GraphQLInputObjectType.newInputObject();
    builder.name(typeName)
        .description("Boolean expression to filter rows from the table \"" + name
            + "\". All fields are combined with a logical 'AND'.")
        .field(GraphQLInputObjectField.newInputObjectField()
            .name("_and")
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(typeName)))
            .build())
        .field(GraphQLInputObjectField.newInputObjectField()
            .name("_not")
            .type(GraphQLTypeReference.typeRef(typeName))
            .build())
        .field(GraphQLInputObjectField.newInputObjectField()
            .name("_or")
            .type(GraphQLList.list(GraphQLTypeReference.typeRef(typeName)))
            .build());
    GraphQLInputType primaryKeyGraphQLType = ConditionalOperatorTypes.scalarTypeToComparisonExpMap
        .get(classGraphQLScalarTypeMap.get(primaryKeyClass));
    builder.field(GraphQLInputObjectField.newInputObjectField()
        .name(primaryKeyName)
        .type(primaryKeyGraphQLType)
        .build());
    columns.forEach((key, value) -> {
      GraphQLInputType columnGraphQLType = ConditionalOperatorTypes.scalarTypeToComparisonExpMap
          .get(classGraphQLScalarTypeMap.get(value.getClazz()));
      builder.field(GraphQLInputObjectField.newInputObjectField()
          .name(key)
          .type(columnGraphQLType)
          .build());
    });
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
