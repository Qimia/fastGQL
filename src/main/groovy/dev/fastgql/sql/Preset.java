package dev.fastgql.sql;

import java.util.Map;
import java.util.function.Function;

public class Preset {
  private final String column;
  private final Function<Map<String, Object>, Object> function;

  public Preset(String column, Function<Map<String, Object>, Object> function) {
    this.column = column;
    this.function = function;
  }

  public String getColumn() {
    return column;
  }

  public Function<Map<String, Object>, Object> getFunction() {
    return function;
  }
}
