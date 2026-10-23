package algorithms;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.*;
public final class Condensation {
    private Condensation() {}
    public static Graph build(Graph g,Kosaraju.Result scc) {
        g.requireDirected(true);
        Graph dag=new Graph(scc.count(),true);
        for(Edge e:g.edges()) {
            int a=scc.component()[e.from()],b=scc.component()[e.to()];
            if(a!=b) dag.addEdge(a,b,e.weight());
        }
        return dag;
    }
    public static DagShortestPath.Result longestPaths(Graph dag) {
        Kahn.Result topo=Kahn.run(dag,new Metrics());
        if(topo.hasCycle()) throw new IllegalArgumentException("A DAG is required");
        long[] d=new long[dag.n()]; int[] parent=new int[dag.n()],in=dag.indegrees();
        Arrays.fill(d,Long.MIN_VALUE); Arrays.fill(parent,-1);
        for(int u=0;u<dag.n();u++) if(in[u]==0) d[u]=0;
        for(int u:topo.order()) for(Edge e:dag.outgoing(u)) {
            long candidate=d[u]+e.weight();
            if(candidate>d[e.to()]) { d[e.to()]=candidate; parent[e.to()]=u; }
        }
        return new DagShortestPath.Result(d,parent);
    }
}