package algorithms;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.Arrays;
public final class Tarjan {
    private Tarjan() {}
    public static Kosaraju.Result run(Graph g,Metrics metrics) {
        g.requireDirected(true); int n=g.n(),clock=0,count=0,activeSize=0;
        int[] index=new int[n],low=new int[n],next=new int[n],parent=new int[n];
        int[] frames=new int[n],active=new int[n],component=new int[n];
        boolean[] onStack=new boolean[n];
        Arrays.fill(index,-1); Arrays.fill(parent,-1); Arrays.fill(component,-1);
        for(int start=0;start<n;start++) {
            if(index[start]!=-1) continue;
            int top=0; frames[0]=start; index[start]=low[start]=clock++;
            active[activeSize++]=start; onStack[start]=true; metrics.depth(1);
            while(top>=0) {
                int u=frames[top];
                if(next[u]<g.outgoing(u).size()) {
                    Edge e=g.outgoing(u).get(next[u]++); metrics.edgeScans++; int v=e.to();
                    if(index[v]==-1) {
                        parent[v]=u; index[v]=low[v]=clock++; frames[++top]=v;
                        active[activeSize++]=v; onStack[v]=true; metrics.depth(top+1);
                    } else if(onStack[v]) low[u]=Math.min(low[u],index[v]);
                } else {
                    top--;
                    if(parent[u]!=-1) low[parent[u]]=Math.min(low[parent[u]],low[u]);
                    if(low[u]==index[u]) {
                        int v;
                        do { v=active[--activeSize]; onStack[v]=false; component[v]=count; } while(v!=u);
                        count++;
                    }
                }
            }
        }
        return new Kosaraju.Result(count,component);
    }
}