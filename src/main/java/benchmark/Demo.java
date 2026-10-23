package benchmark;
import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.Path;
import java.util.Arrays;
public final class Demo {
    public static void main(String[] args) throws Exception {
        Graph roads=Graph.read(Path.of("data/disconnected.txt"));
        System.out.println("Kruskal forest: "+MST.kruskal(roads,new Metrics()));
        System.out.println("Prim forest: "+MST.prim(roads,new Metrics()));
        Graph cyclic=Graph.read(Path.of("data/cyclic.txt"));
        Kosaraju.Result scc=Kosaraju.run(cyclic,new Metrics());
        System.out.println("SCC labels: "+Arrays.toString(scc.component()));
        System.out.println("Cycle: "+Kahn.run(cyclic,new Metrics()).hasCycle());
        Graph dag=Graph.read(Path.of("data/dag.txt"));
        System.out.println("Topological order: "+Arrays.toString(Kahn.run(dag,new Metrics()).order()));
        DagShortestPath.Result sp=DagShortestPath.run(dag,new Metrics());
        System.out.println("Distances: "+Arrays.toString(sp.distance()));
        System.out.println("Path to 5: "+sp.pathTo(5));
        Graph condensation=Condensation.build(cyclic,scc);
        DagShortestPath.Result longest=Condensation.longestPaths(condensation);
        int target=0;
        for(int i=1;i<condensation.n();i++) if(longest.distance()[i]>longest.distance()[target]) target=i;
        System.out.println("Condensation critical path (component IDs): "+longest.pathTo(target));
        System.out.println("Critical path weight: "+longest.distance()[target]);
    }
}