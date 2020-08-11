package dev.fastgql.common;

public class TableWithAlias {
  private final String name;
  private final String alias;

  public TableWithAlias(String name, String alias) {
    this.name = name;
    this.alias = alias;
  }

  public String getName() {
    return name;
  }

  public String getAlias() {
    return alias;
  }

  @Override
  public String toString() {
    return "TableWithAlias{" + "name='" + name + '\'' + ", alias='" + alias + '\'' + '}';
  }
}
