package spy.g1;

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Vertex extends spy.sim.Point {
  public String name;
  public boolean known;
  public double distance; // total distance from origin point
  public Vertex prev;
  public Map<String, Edge> adjacentEdges;
  public boolean explored;

  public Vertex(String name, int x, int y) {
    super(x,y);
    this.name = name;

    // by default java sets uninitialized boolean to false and double to 0
    // hence known == false and dist == 0.0 and explored == false
    adjacentEdges = new HashMap<String, Edge>();
    prev = null;
  }

  // @Override
  // public int hashCode() {
  //   // we assume that each vertex has a unique name
  //   return name.hashCode();
  // }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null) {
      return false;
    }
    if (!(o instanceof Vertex)) {
      return false;
    }
    Vertex oVertex = (Vertex) o;

    return name.equals(oVertex.name) && x == oVertex.x && y == oVertex.y;
  }

  public void setEdge(Edge edge) {
    adjacentEdges.put(edge.target.name, edge);
  }

  @Override
  public String toString() {
    return name + " (" + x + ", " + y + ")";
  }

}