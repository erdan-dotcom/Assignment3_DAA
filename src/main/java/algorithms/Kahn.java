package algorithms;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.*;
public final class Kahn {
    private Kahn() {}
    public record Result(boolean hasCycle,int[] order) {}
    public static Result run(Graph g,Metrics metrics) {
        g.requireDirected(true);
        int[] in=g.indegrees(), order=new int[g.n()]; int length=0;
        ArrayDeque<Integer> queue=new ArrayDeque<>();
        for(int u=0;u<g.n();u++) if(in[u]==0) queue.add(u);
        while(!queue.isEmpty()) {
            int u=queue.remove(); order[length++]=u;
            for(Edge e:g.outgoing(u)) {
                metrics.edgeScans++;
                if(--in[e.to()]==0) queue.add(e.to());
            }
        }
        if(length!=g.n()) return new Result(true,new int[0]);
        return new Result(false,order);
    }
    public static boolean isValid(Graph g,int[] order) {
        if(!g.directed() || order.length!=g.n()) return false;
        int[] pos=new int[g.n()]; Arrays.fill(pos,-1);
        for(int i=0;i<order.length;i++) {
            int u=order[i];
            if(u<0 || u>=g.n() || pos[u]!=-1) return false;
            pos[u]=i;
        }
        for(Edge e:g.edges()) if(pos[e.from()]>=pos[e.to()]) return false;
        return true;
    }
}