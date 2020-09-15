package dev.fastgql.sql;

import dev.fastgql.db.DatasourceConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;

public class PreparedQuery {

  enum ElementType {
    normal,
    placeholder
  }

  static class Element {
    private final ElementType type;
    private final String content;

    Element(ElementType type, String content) {
      this.type = type;
      this.content = content;
    }

    @Override
    public String toString() {
      return "Element{" +
        "type=" + type +
        ", content='" + content + '\'' +
        '}';
    }
  }

  private final List<Element> elements;
  private final List<Object> params;

  private PreparedQuery() {
    this.elements = new ArrayList<>();
    this.params = new ArrayList<>();
  }

  private PreparedQuery(String query) {
    this.elements = new ArrayList<>();
    this.params = new ArrayList<>();
    if (!query.isEmpty()) {
      this.elements.add(new Element(ElementType.normal, query));
    }
  }

  static PreparedQuery create(String query) {
    return new PreparedQuery(query);
  }

  static PreparedQuery create() {
    return new PreparedQuery();
  }

  boolean isEmpty() {
    return elements.isEmpty();
  }

  PreparedQuery addParam(Object object) {
    elements.add(new Element(ElementType.placeholder, ""));
    params.add(object);
    return this;
  }

  PreparedQuery merge(PreparedQuery other) {
    elements.addAll(other.elements);
    params.addAll(other.params);
    return this;
  }

  PreparedQuery merge(String query) {
    elements.add(new Element(ElementType.normal, query));
    return this;
  }

  public String buildQuery(DatasourceConfig.DBType dbType) {
    StringBuilder builder = new StringBuilder();
    AtomicInteger counter = new AtomicInteger(1);
    elements.forEach(element -> {
      switch (element.type) {
        case normal:
          builder.append(element.content);
          break;
        case placeholder:
          switch (dbType) {
            case postgresql:
              builder.append("$").append(counter.getAndIncrement());
              break;
            case mysql:
              builder.append("?");
              break;
            case other:
              throw new RuntimeException("DB type not supported");
          }
      }
    });
    return builder.toString();
  }

  public List<Object> getParams() {
    return params;
  }

  @Override
  public String toString() {
    return "PreparedQuery{" +
      "queryBuilder=" + elements +
      ", params=" + params +
      '}';
  }

  private static void accumulator(PreparedQuery first, PreparedQuery second) {
    first.elements.addAll(second.elements);
    first.params.addAll(second.params);
  }

  public static Collector<PreparedQuery, PreparedQuery, PreparedQuery> collector() {
    return Collector.of(
      PreparedQuery::new,
      PreparedQuery::accumulator,
      (first, second) -> {
        accumulator(first, second);
        return first;
      }
    );
  }

  public static Collector<PreparedQuery, ArrayList<PreparedQuery>, PreparedQuery> collectorWithAnd() {
    return Collector.of(
      ArrayList::new,
      ArrayList::add,
      (first, second) -> {
        first.addAll(second);
        return first;
      },
      array -> array
        .stream()
        .filter(preparedQuery -> !preparedQuery.isEmpty())
        .map(preparedQuery -> PreparedQuery.create("(").merge(preparedQuery).merge(")"))
        .reduce((first, second) -> {
          first.merge(PreparedQuery.create(" AND ").merge(second));
          return first;
        }).orElse(PreparedQuery.create())
    );
  }
}
