package algorithms;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.*;
public final class DagShortestPath {
    private DagShortestPath() {}
    public record Result(long[] distance,int[] parent) {
        public long sum() { long s=0; for(long d:distance) s+=d; return s; }
        public List<Integer> pathTo(int vertex) {
            if(vertex<0 || vertex>=parent.length) throw new IllegalArgumentException("Invalid vertex");
            List<Integer> path=new ArrayList<>();
            for(int u=vertex;u!=-1;u=parent[u]) path.add(u);
            Collections.reverse(path); return path;
        }
    }
    public static Result run(Graph g,Metrics metrics) {
        Kahn.Result topo=Kahn.run(g,new Metrics());
        if(topo.hasCycle()) throw new IllegalArgumentException("Shortest paths require a DAG");
        return run(g,topo.order(),metrics);
    }
    public static Result run(Graph g,int[] order,Metrics metrics) {
        g.requireDirected(true);
        if(!Kahn.isValid(g,order)) throw new IllegalArgumentException("Invalid topological order");
        int[] in=g.indegrees(),parent=new int[g.n()];
        long[] distance=new long[g.n()];
        Arrays.fill(parent,-1); Arrays.fill(distance,Long.MAX_VALUE);
        for(int u=0;u<g.n();u++) if(in[u]==0) distance[u]=0;
        for(int u:order) {
            for(Edge e:g.outgoing(u)) {
                metrics.edgeScans++;
                long candidate=distance[u]+e.weight();
                metrics.comparisons++;
                if(candidate<distance[e.to()]) {
                    distance[e.to()]=candidate; parent[e.to()]=u;
                }
            }
        }
        return new Result(distance,parent);
    }
}