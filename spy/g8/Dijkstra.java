package spy.g8;

import java.util.ArrayList;
import java.util.List;

public class Dijkstra {

    // Dijkstra's algorithm to find shortest path from s to all other nodes
    public static int[][] dijkstra(WeightedGraph G, int s) {
        double[] dist = new double[G.size()];  // shortest known distance from "s"
        boolean[] visited = new boolean[G.size()]; // all false initially

        // Contains nodes that appear before current node in the shorted path from
        //  source to the node.
        int[][] prev = new int[G.size()][G.size()];

        for (int i=0; i<dist.length; i++) {
            dist[i] = Double.MAX_VALUE;
            prev[i][0] = 0;
        }

        dist[s] = 0.;

        for (int i=0; i<dist.length; i++) {
            final int next = minVertex(dist, visited);
            // System.out.println("Next: " + next + " i: " + G.getLabel(i));
            
            if (next == -1) {
                continue;
            }

            visited[next] = true;

            // The shortest path to next is dist[next] and via pred[next].
            final int[] n = G.neighbors(next);
            for (int j=0; j<n.length; j++) {
                final int v = n[j];
                final double d = dist[next] + G.getWeight(next,v);
                if (dist[v] > d) { // Path of lesser weight is found.
                    dist[v] = d;

                    prev[v][0] = 1;
                    prev[v][1] = next;
                }
                else if (dist[v] == d) { // There is a path of the same weight.
                    ++prev[v][0];
                    prev[v][prev[v][0]] = next;
                }
            }
        }

        return prev;  // (ignore pred[s]==0!)
    }

    private static int minVertex (double[] dist, boolean[] v) {
        double x = Double.MAX_VALUE;
        int y = -1;    // graph not connected, or no unvisited vertices
        for (int i=0; i<dist.length; i++) {
            if (!v[i] && dist[i]<x) {y=i; x=dist[i];}
        }
        return y;
    }

    public static void printPrev(int[][] prev) {
        for (int i=0; i<prev.length; ++i) {
            for (int j=0; j<prev[i].length; ++j) {
                System.out.print(prev[i][j] + " ");
            }

            System.out.println();
        }
    }

    public static void printPath(WeightedGraph G, int[][] prev, int dest) {
        List<String> allPaths = new ArrayList<>();
        getpaths(G, prev, dest, "", allPaths);

        for (String p : allPaths) {
            System.out.println(p);
        }
    }

    public static List<String> getLabelPaths(WeightedGraph G, int[][] prev, int dest) {
        List<String> allPaths = new ArrayList<>();
        getpaths(G, prev, dest, "", allPaths);

        return allPaths;
    }

    public static List<List<Integer>> getPaths(WeightedGraph G, int[][] prev, int dest) {
        List<Integer> paths = new ArrayList<>();
        List<List<Integer>> allPaths = new ArrayList<>();

        getpaths(G, prev, dest, paths, allPaths);

        return allPaths;
    }

    private static void getpaths(WeightedGraph G, int[][] prev, int dest,
        List<Integer> path, List<List<Integer>> allPaths) {

        path = new ArrayList<>(path);
        path.add(0, dest);

        if (prev[dest][0] == 0) {
            allPaths.add(path);

            return;
        }

        for (int i=1; i<=prev[dest][0]; ++i) {
            getpaths(G, prev, prev[dest][i], path, allPaths);
        }
    }

    private static void getpaths(WeightedGraph G, int[][] prev, int dest, String path,
        List<String> allPaths) {

        path = G.getLabel(dest) + " " + path;

        if (prev[dest][0] == 0) {
            allPaths.add(path);

            return;
        }

        for (int i=1; i<=prev[dest][0]; ++i) {
            getpaths(G, prev, prev[dest][i], path, allPaths);
        }
    }
}







