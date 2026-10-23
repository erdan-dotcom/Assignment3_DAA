package benchmark;
import graph.Graph;
import java.util.Random;
public final class Generator {
    private Generator() {}
    public enum Kind { UNDIRECTED, DIRECTED, DAG }
    public static Graph generate(int n,int m,Kind kind) {
        if(n<0 || m<0 || (n<2 && m>0) || (kind==Kind.UNDIRECTED && m<Math.max(0,n-1)))
            throw new IllegalArgumentException("Impossible generator parameters");
        Random rnd=new Random(42);
        Graph g=new Graph(n,kind!=Kind.UNDIRECTED);
        int[] perm=new int[n]; for(int i=0;i<n;i++) perm[i]=i;
        if(kind!=Kind.DIRECTED) {
            for(int i=n-1;i>0;i--) {
                int j=rnd.nextInt(i+1),t=perm[i]; perm[i]=perm[j]; perm[j]=t;
            }
        }
        if(kind==Kind.UNDIRECTED) {
            for(int i=1;i<n;i++) {
                int u=perm[rnd.nextInt(i)],v=perm[i];
                g.addEdge(u,v,1+rnd.nextInt(1000));
            }
        }
        while(g.m()<m) {
            int a=rnd.nextInt(n),b=rnd.nextInt(n);
            while(a==b) b=rnd.nextInt(n);
            if(kind==Kind.DAG && perm[a]>perm[b]) { int t=a; a=b; b=t; }
            g.addEdge(a,b,1+rnd.nextInt(1000));
        }
        return g;
    }
}