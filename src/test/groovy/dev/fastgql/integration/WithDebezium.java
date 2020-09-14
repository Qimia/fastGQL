package dev.fastgql.integration;

public interface WithDebezium extends WithFastGQL {
  @Override
  default boolean isDebeziumActive() {
    return true;
  }
}
