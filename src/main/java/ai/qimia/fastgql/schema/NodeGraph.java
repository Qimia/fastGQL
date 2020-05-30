package ai.qimia.fastgql.schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NodeGraph {
  private Map<String, NodeDefinition> graph;

  public NodeGraph() {
    this.graph = new HashMap<>();
  }

  public void addNode(NodeDefinition newNode) {
    QualifiedName qualifiedName = newNode.getQualifiedName();
    String key = qualifiedName.toString();
    if (graph.containsKey(key)) {
      graph.get(key).merge(newNode);
    } else {
      graph.put(key, newNode);
    }
    QualifiedName referencing = newNode.getReferencing();
    if (referencing != null) {
      addNode(new NodeDefinition(referencing, newNode.getNodeType(), null, Set.of(qualifiedName)));
    }
  }

  @Override
  public String toString() {
    return "NodeGraph{" +
      "graph=" + graph +
      '}';
  }
}
