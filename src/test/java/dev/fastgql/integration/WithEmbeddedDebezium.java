package dev.fastgql.integration;

public interface WithEmbeddedDebezium extends WithDebezium {
  @Override
  default boolean isDebeziumEmbedded() {
    return true;
  };
}
