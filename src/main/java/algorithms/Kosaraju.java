package algorithms;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.*;
public final class Kosaraju {
    private Kosaraju() {}
    public record Result(int count, int[] component) {}
    public static Result run(Graph g, Metrics metrics) {
        g.requireDirected(true);
        int n=g.n(),finished=0;
        boolean[] seen=new boolean[n];
        int[] order=new int[n], stack=new int[n], next=new int[n];
        for(int start=0;start<n;start++) {
            if(seen[start]) continue;
            int top=0; stack[0]=start; seen[start]=true; metrics.depth(1);
            while(top>=0) {
                int u=stack[top];
                if(next[u]<g.outgoing(u).size()) {
                    Edge e=g.outgoing(u).get(next[u]++); metrics.edgeScans++;
                    if(!seen[e.to()]) {
                        seen[e.to()]=true; stack[++top]=e.to(); metrics.depth(top+1);
                    }
                } else { order[finished++]=u; top--; }
            }
        }
        // Transposition is graph preparation; edgeScans counts the two DFS passes.
        Graph reversed=g.reverse();
        int[] component=new int[n]; Arrays.fill(component,-1); Arrays.fill(next,0);
        int count=0;
        for(int i=n-1;i>=0;i--) {
            int start=order[i]; if(component[start]!=-1) continue;
            int top=0; stack[0]=start; component[start]=count; metrics.depth(1);
            while(top>=0) {
                int u=stack[top];
                if(next[u]<reversed.outgoing(u).size()) {
                    Edge e=reversed.outgoing(u).get(next[u]++); metrics.edgeScans++;
                    if(component[e.to()]==-1) {
                        component[e.to()]=count; stack[++top]=e.to(); metrics.depth(top+1);
                    }
                } else top--;
            }
            count++;
        }
        return new Result(count,component);
    }
}