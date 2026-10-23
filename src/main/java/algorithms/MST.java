package algorithms;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import java.util.*;
public final class MST {
    private MST() {}
    public record Result(long weight, List<Edge> edges, int components) {}
    public static Result kruskal(Graph g, Metrics metrics) {
        g.requireDirected(false);
        Edge[] a=g.edges().toArray(Edge[]::new);
        mergeSort(a,metrics);
        DSU dsu=new DSU(g.n(),metrics);
        List<Edge> tree=new ArrayList<>(); long total=0;
        for(Edge e:a) {
            if(dsu.components()<=1) break;
            metrics.edgeScans++;
            if(dsu.find(e.from())!=dsu.find(e.to())) {
                dsu.union(e.from(),e.to()); tree.add(e); total+=e.weight();
            }
        }
        return new Result(total,List.copyOf(tree),dsu.components());
    }
    // Own bottom-up stable merge sort: reproducible weight-comparison count.
    private static void mergeSort(Edge[] a, Metrics metrics) {
        Edge[] aux=new Edge[a.length];
        for(int width=1;width<a.length;width*=2) {
            for(int left=0;left<a.length;left+=2*width) {
                int mid=Math.min(left+width,a.length), right=Math.min(left+2*width,a.length);
                int i=left,j=mid,k=left;
                while(i<mid && j<right) {
                    metrics.comparisons++;
                    aux[k++]=(a[i].weight()<=a[j].weight())?a[i++]:a[j++];
                }
                while(i<mid) aux[k++]=a[i++];
                while(j<right) aux[k++]=a[j++];
            }
            System.arraycopy(aux,0,a,0,a.length);
        }
    }
    public static Result prim(Graph g, Metrics metrics) {
        g.requireDirected(false);
        boolean[] used=new boolean[g.n()];
        PriorityQueue<Edge> queue=new PriorityQueue<>((a,b)-> {
            metrics.comparisons++;
            int c=Integer.compare(a.weight(),b.weight());
            if(c!=0) return c;
            c=Integer.compare(a.from(),b.from());
            return c!=0?c:Integer.compare(a.to(),b.to());
        });
        List<Edge> tree=new ArrayList<>(); long total=0; int components=0;
        // Start at 0; restart only when the current connected component is complete.
        for(int start=0;start<g.n();start++) {
            if(used[start]) continue;
            components++; queue.add(new Edge(-1,start,0));
            while(!queue.isEmpty()) {
                Edge best=queue.remove(); int u=best.to();
                if(used[u]) continue;
                used[u]=true;
                if(best.from()!=-1) { tree.add(best); total+=best.weight(); }
                for(Edge e:g.outgoing(u)) {
                    metrics.edgeScans++;
                    if(!used[e.to()]) queue.add(e);
                }
            }
        }
        return new Result(total,List.copyOf(tree),components);
    }
}