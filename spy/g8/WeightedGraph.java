package spy.g8;

import java.util.Arrays;

public class WeightedGraph {

    private double[][] edges;  // adjacency matrix
    private String[] labels;

    private int counter = 0;

    public WeightedGraph(int n) {
        edges  = new double[n][n];
        labels = new String[n];

        for (double[] row : edges) {
            Arrays.fill(row, -1.);
        }
    }

    public int size() {
        return labels.length;
    }

    public void setLabel(String label) {
        labels[counter++] = label;
    }

    public String getLabel(int vertex) {
        return labels[vertex];
    }

    public int getVertex(String label) {
        // Ridiculous.
        return Arrays.asList(labels).indexOf(label);
    }

    public void addEdge(int source, int target, double w) {
        addDirectedEdge(source, target, w);
        addDirectedEdge(target, source, w);
    }

    public void addEdge(String source, String target, double w) {
        addDirectedEdge(source, target, w);
        addDirectedEdge(target, source, w);
    }

    public void addDirectedEdge(int source, int target, double w) {
        edges[source][target] = w;
    }

    public void addDirectedEdge(String source, String target, double w) {
        edges[getVertex(source)][getVertex(target)] = w;
    }

    public boolean isEdge(int source, int target) {
        return edges[source][target] >= 0; 
    }
    
    public void removeEdge(int source, int target) {
        edges[source][target] = -1;
    }
    
    public double getWeight(int source, int target) {
        return edges[source][target];
    }

    public int[] neighbors(int vertex) {
        int count = 0;
        for (int i=0; i<edges[vertex].length; i++) {
            if (edges[vertex][i]>=0.) count++;
        }

        final int[] answer = new int[count];
        count = 0;
        for (int i=0; i<edges[vertex].length; i++) {
            if (edges[vertex][i]>=0.) answer[count++]=i;
        }

        return answer;
    }

    public void print() {
        for (int j=0; j<edges.length; j++) {
            System.out.print(labels[j]+": ");
            for (int i=0; i<edges[j].length; i++) {
                if (edges[j][i]>=0)
                    System.out.print(labels[i]+":"+edges[j][i]+" ");
            }

            System.out.println();
        }
    }

    public static void main(String args[]) {
        final WeightedGraph t = new WeightedGraph(6);
        t.setLabel("A");
        t.setLabel("B");
        t.setLabel("C");
        t.setLabel("D");
        t.setLabel("E");
        t.setLabel("F");

        t.addEdge(0, 1, 100);
        t.addEdge(0, 3, 50);
        t.addEdge(1, 2, 100);
        t.addEdge(2, 4, 50);
        t.addEdge(2, 5, 100);
        t.addEdge(3, 4, 200);

        t.print();

        int s = 0;
        final int[][] pred = Dijkstra.dijkstra(t, s);
        for (int n=s; n<6; n++) {
            System.out.println(t.getLabel(s) + " to " + t.getLabel(n));
            Dijkstra.printPath(t, pred, n);
            System.out.println();
        }
    }
}
