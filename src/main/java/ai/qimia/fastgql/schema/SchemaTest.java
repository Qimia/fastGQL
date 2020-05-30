package ai.qimia.fastgql.schema;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SchemaTest {
  public static void main(String[] args) {
    NodeGraph graph = new NodeGraph();
    graph.addNode(new NodeDefinition(new QualifiedName("customers"), NodeType.TABLE, null, null));
    graph.addNode(new NodeDefinition(new QualifiedName("customers/id"), NodeType.INT, null, null));
    graph.addNode(new NodeDefinition(new QualifiedName("customers/address"), NodeType.INT, new QualifiedName("addresses/id"), null));
    graph.addNode(new NodeDefinition(new QualifiedName("addresses"), NodeType.TABLE, null, null));
    graph.addNode(new NodeDefinition(new QualifiedName("addresses/id"), NodeType.INT, null, null));
    System.out.println(graph);
  }
}
