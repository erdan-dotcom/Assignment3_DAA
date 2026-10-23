package graph;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** One adjacency-list type. edges() contains each input edge once. */
public final class Graph {
    public record Edge(int from, int to, int weight) {}
    private final int n;
    private final boolean directed;
    private final List<Edge>[] adj;
    private final List<Edge> edges = new ArrayList<>();
    private final int[] indegree;
    @SuppressWarnings("unchecked")
    public Graph(int n, boolean directed) {
        if (n < 0) throw new IllegalArgumentException("Negative vertex count");
        this.n=n; this.directed=directed;
        adj=(List<Edge>[])new List<?>[n]; indegree=new int[n];
        for(int i=0;i<n;i++) adj[i]=new ArrayList<>();
    }
    public int n() { return n; }
    public int m() { return edges.size(); }
    public boolean directed() { return directed; }
    public void checkVertex(int v) {
        if(v<0 || v>=n) throw new IllegalArgumentException("Invalid vertex: "+v);
    }
    public void addEdge(int u,int v,int w) {
        checkVertex(u); checkVertex(v);
        Edge e=new Edge(u,v,w); edges.add(e); adj[u].add(e); indegree[v]++;
        if(!directed) { adj[v].add(new Edge(v,u,w)); indegree[u]++; }
    }
    public List<Edge> outgoing(int v) { return Collections.unmodifiableList(adj[v]); }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public int[] indegrees() { return indegree.clone(); }
    public Graph reverse() {
        Graph r=new Graph(n,directed);
        for(Edge e:edges) r.addEdge(e.to(),e.from(),e.weight());
        return r;
    }
    public void requireDirected(boolean expected) {
        if(directed!=expected) throw new IllegalArgumentException(expected?"Directed graph required":"Undirected graph required");
    }
    public static Graph read(Path path) throws IOException {
        try(Scanner in=new Scanner(Files.newBufferedReader(path))) {
            int n=in.nextInt(), m=in.nextInt(); String kind=in.next();
            if(m<0 || (!kind.equals("directed") && !kind.equals("undirected")))
                throw new IllegalArgumentException("Invalid header");
            Graph g=new Graph(n,kind.equals("directed"));
            for(int i=0;i<m;i++) g.addEdge(in.nextInt(),in.nextInt(),in.nextInt());
            if(in.hasNext()) throw new IllegalArgumentException("Unexpected trailing input");
            return g;
        } catch(NoSuchElementException ex) { throw new IOException("Invalid graph file: "+path,ex); }
    }
}