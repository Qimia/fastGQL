package ai.qimia.fastgql.sql;

public interface ComponentParent {
  void addComponent(Component component);
  String trueTableNameWhenParent();
}
