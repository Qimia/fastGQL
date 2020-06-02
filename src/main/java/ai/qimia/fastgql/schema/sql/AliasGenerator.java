package ai.qimia.fastgql.schema.sql;

public class AliasGenerator {
  private long counter = 0;

  public String getAlias() {
    return String.format("a%d", counter++);
  }
}
