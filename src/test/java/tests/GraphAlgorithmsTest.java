package tests;
import algorithms.*;
import benchmark.*;
import graph.Graph;
import graph.Graph.Edge;
import metrics.Metrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GraphAlgorithmsTest {
    @Test void dsuAgreesWithReferenceLabels() {
        int n=100; int[] label=new int[n]; for(int i=0;i<n;i++) label[i]=i;
        Metrics metrics=new Metrics(); DSU d=new DSU(n,metrics); Random r=new Random(42);
        for(int step=0;step<1000;step++) {
            int a=r.nextInt(n),b=r.nextInt(n),old=label[b],replacement=label[a];
            assertEquals(old!=replacement,d.union(a,b));
            for(int i=0;i<n;i++) if(label[i]==old) label[i]=replacement;
            for(int i=0;i<n;i++) for(int j=0;j<n;j++)
                assertEquals(label[i]==label[j],d.find(i)==d.find(j));
            assertEquals(Arrays.stream(label).distinct().count(),d.components());
        }
        assertEquals(n-d.components(),metrics.successfulUnions);
        assertEquals(2*1000L+2*1000L*n*n,metrics.findCalls);
        assertEquals(metrics.findCalls+metrics.successfulUnions,metrics.dsuOps());
        d.makeSet(5); assertEquals(5,d.components());
    }
    @Test void mstOn100RandomConnectedGraphs() {
        Random r=new Random(42);
        for(int trial=0;trial<100;trial++) {
            int n=2+r.nextInt(49); Graph g=new Graph(n,false);
            for(int i=1;i<n;i++) g.addEdge(r.nextInt(i),i,r.nextInt(101)-50);
            for(int i=0;i<4*n;i++) g.addEdge(r.nextInt(n),r.nextInt(n),r.nextInt(101)-50);
            MST.Result a=MST.kruskal(g,new Metrics()),b=MST.prim(g,new Metrics());
            assertEquals(a.weight(),b.weight()); assertEquals(n-1,a.edges().size());
            assertEquals(n-1,b.edges().size()); assertEquals(1,a.components());
            verifyForest(g,a); verifyForest(g,b);
        }
    }
    private static void verifyForest(Graph g,MST.Result result) {
        DSU d=new DSU(g.n(),new Metrics()); long sum=0;
        for(Edge e:result.edges()) { assertTrue(d.union(e.from(),e.to())); sum+=e.weight(); }
        assertEquals(result.weight(),sum); assertEquals(result.components(),d.components());
        for(Edge e:g.edges()) assertEquals(d.find(e.from()),d.find(e.to()));
    }
    @Test void mstAlsoAgreesWithExhaustiveSubsets() {
        Random r=new Random(13);
        for(int trial=0;trial<30;trial++) {
            Graph g=new Graph(5,false);
            for(int i=1;i<5;i++) g.addEdge(i-1,i,r.nextInt(20)-10);
            for(int i=0;i<4;i++) g.addEdge(r.nextInt(5),r.nextInt(5),r.nextInt(20)-10);
            long best=Long.MAX_VALUE;
            for(int mask=0;mask<(1<<g.m());mask++) {
                if(Integer.bitCount(mask)!=4) continue;
                DSU d=new DSU(5,new Metrics()); long sum=0; boolean ok=true;
                for(int i=0;i<g.m();i++) if((mask&(1<<i))!=0) {
                    Edge e=g.edges().get(i); if(!d.union(e.from(),e.to())) {ok=false; break;} sum+=e.weight();
                }
                if(ok && d.components()==1) best=Math.min(best,sum);
            }
            assertEquals(best,MST.kruskal(g,new Metrics()).weight());
            assertEquals(best,MST.prim(g,new Metrics()).weight());
        }
    }
    private static boolean[] bfs(Graph g,int start) {
        boolean[] seen=new boolean[g.n()]; ArrayDeque<Integer> q=new ArrayDeque<>();
        q.add(start); seen[start]=true;
        while(!q.isEmpty()) for(Edge e:g.outgoing(q.remove()))
            if(!seen[e.to()]) { seen[e.to()]=true; q.add(e.to()); }
        return seen;
    }
    private static void verifyScc(Graph g) {
        Metrics km=new Metrics(),tm=new Metrics();
        Kosaraju.Result k=Kosaraju.run(g,km),t=Tarjan.run(g,tm);
        assertTrue(Benchmark.samePartition(k,t));
        assertEquals(2L*g.m(),km.edgeScans); assertEquals(g.m(),tm.edgeScans);
        Graph reversed=g.reverse();
        for(int u=0;u<g.n();u++) {
            boolean[] forward=bfs(g,u),backward=bfs(reversed,u);
            for(int v=0;v<g.n();v++) assertEquals(forward[v]&&backward[v],k.component()[u]==k.component()[v]);
        }
    }
    @Test void sccAgainstBfsBothDirections() {
        Random r=new Random(42);
        for(int trial=0;trial<40;trial++) {
            int n=1+r.nextInt(200); Graph g=new Graph(n,true);
            int m=r.nextInt(6)*n;
            for(int i=0;i<m;i++) g.addEdge(r.nextInt(n),r.nextInt(n),1);
            verifyScc(g);
        }
    }
    @Test void topologicalOrderAndCycleDetection() throws Exception {
        Graph dag=Graph.read(Path.of("data/dag.txt")); Metrics metrics=new Metrics();
        Kahn.Result result=Kahn.run(dag,metrics);
        assertFalse(result.hasCycle()); assertTrue(Kahn.isValid(dag,result.order()));
        assertEquals(dag.m(),metrics.edgeScans);
        assertFalse(Kahn.isValid(dag,new int[]{0,0,2,3,4,5}));
        Graph cyclic=Graph.read(Path.of("data/cyclic.txt"));
        assertTrue(Kahn.run(cyclic,new Metrics()).hasCycle());
        assertEquals(0,Kahn.run(cyclic,new Metrics()).order().length);
        assertThrows(IllegalArgumentException.class,()->DagShortestPath.run(cyclic,new Metrics()));
        verifyScc(cyclic);
        Graph self=new Graph(1,true); self.addEdge(0,0,0);
        assertTrue(Kahn.run(self,new Metrics()).hasCycle()); verifyScc(self);
    }
    private static long[] bellmanFord(Graph g) {
        long[] d=new long[g.n()]; Arrays.fill(d,Long.MAX_VALUE);
        int[] in=g.indegrees(); for(int u=0;u<g.n();u++) if(in[u]==0) d[u]=0;
        for(int pass=0;pass<g.n()-1;pass++) {
            boolean changed=false;
            for(Edge e:g.edges()) if(d[e.from()]!=Long.MAX_VALUE && d[e.to()]>d[e.from()]+e.weight()) {
                d[e.to()]=d[e.from()]+e.weight(); changed=true;
            }
            if(!changed) break;
        }
        return d;
    }
    @Test void shortestPathsAgainstBellmanFordWithNegativeWeights() {
        Random r=new Random(42);
        for(int trial=0;trial<100;trial++) {
            int n=1+r.nextInt(40); Graph g=new Graph(n,true);
            List<Integer> perm=new ArrayList<>(); for(int i=0;i<n;i++) perm.add(i);
            Collections.shuffle(perm,r);
            for(int i=0;i<n;i++) for(int j=i+1;j<n;j++) if(r.nextDouble()<0.15)
                g.addEdge(perm.get(i),perm.get(j),r.nextInt(101)-50);
            Metrics m=new Metrics(); DagShortestPath.Result result=DagShortestPath.run(g,m);
            assertArrayEquals(bellmanFord(g),result.distance()); assertEquals(g.m(),m.edgeScans);
            int[] in=g.indegrees();
            for(int v=0;v<n;v++) {
                List<Integer> path=result.pathTo(v); assertEquals(v,path.get(path.size()-1));
                assertEquals(0,in[path.get(0)]);
                for(int i=1;i<path.size();i++) {
                    int u=path.get(i-1),w=path.get(i);
                    assertTrue(g.outgoing(u).stream().anyMatch(e->e.to()==w && result.distance()[u]+e.weight()==result.distance()[w]));
                }
            }
        }
    }
    @Test void dataFilesAndForestEdgeCases() throws Exception {
        Graph connected=Graph.read(Path.of("data/connected.txt"));
        assertEquals(10,MST.kruskal(connected,new Metrics()).weight());
        assertEquals(10,MST.prim(connected,new Metrics()).weight());
        Graph g=Graph.read(Path.of("data/disconnected.txt"));
        for(MST.Result result:List.of(MST.kruskal(g,new Metrics()),MST.prim(g,new Metrics()))) {
            assertEquals(5,result.weight()); assertEquals(3,result.components());
            assertEquals(3,result.edges().size()); verifyForest(g,result);
        }
        Graph dag=Graph.read(Path.of("data/dag.txt"));
        DagShortestPath.Result result=DagShortestPath.run(dag,new Metrics());
        assertArrayEquals(new long[]{0,0,2,-3,-2,0},result.distance());
        assertEquals(List.of(1,2,3,4,5),result.pathTo(5));
    }
    @Test void stackSafety100000Vertices() {
        Graph g=new Graph(100000,true);
        for(int i=0;i<g.n()-1;i++) g.addEdge(i,i+1,1);
        Metrics km=new Metrics(),tm=new Metrics();
        Kosaraju.Result k=assertDoesNotThrow(()->Kosaraju.run(g,km));
        Kosaraju.Result t=assertDoesNotThrow(()->Tarjan.run(g,tm));
        assertEquals(100000,k.count()); assertTrue(Benchmark.samePartition(k,t));
        assertEquals(100000,km.maxDepth); assertEquals(100000,tm.maxDepth);
    }
    @Test void emptySingletonAndLargeWeights() {
        for(int n:new int[]{0,1,5}) {
            Graph u=new Graph(n,false),d=new Graph(n,true);
            assertEquals(n,MST.kruskal(u,new Metrics()).components());
            assertEquals(n,MST.prim(u,new Metrics()).components());
            verifyScc(d); assertEquals(n,Kahn.run(d,new Metrics()).order().length);
            assertEquals(0,DagShortestPath.run(d,new Metrics()).sum());
        }
        Graph g=new Graph(3,true); g.addEdge(0,1,Integer.MAX_VALUE); g.addEdge(1,2,Integer.MAX_VALUE);
        assertEquals(2L*Integer.MAX_VALUE,DagShortestPath.run(g,new Metrics()).distance()[2]);
        Graph u=new Graph(3,false); u.addEdge(0,1,Integer.MAX_VALUE); u.addEdge(1,2,Integer.MAX_VALUE);
        assertEquals(2L*Integer.MAX_VALUE,MST.kruskal(u,new Metrics()).weight());
    }
    @Test void condensationAndCriticalPath() throws Exception {
        Graph g=Graph.read(Path.of("data/cyclic.txt")); Kosaraju.Result s=Kosaraju.run(g,new Metrics());
        Graph dag=Condensation.build(g,s);
        assertEquals(3,dag.n()); assertEquals(1,dag.m());
        assertTrue(Kahn.isValid(dag,Kahn.run(dag,new Metrics()).order()));
        assertEquals(5,Arrays.stream(Condensation.longestPaths(dag).distance()).max().orElseThrow());
        Graph d=Graph.read(Path.of("data/dag.txt"));
        assertArrayEquals(new long[]{0,0,4,7,8,10},Condensation.longestPaths(d).distance());
        Graph negative=new Graph(2,true); negative.addEdge(0,1,-5);
        assertArrayEquals(new long[]{0,-5},Condensation.longestPaths(negative).distance());
    }
    @Test void generatorReferenceSmallest() {
        Graph u=Generator.generate(1000,4000,Generator.Kind.UNDIRECTED);
        Graph d=Generator.generate(1000,4000,Generator.Kind.DIRECTED);
        Graph a=Generator.generate(1000,4000,Generator.Kind.DAG);
        assertEquals(147970,MST.kruskal(u,new Metrics()).weight());
        assertEquals(27,Kosaraju.run(d,new Metrics()).count());
        assertEquals(396274,DagShortestPath.run(a,new Metrics()).sum());
    }
    @Test void invalidInputAndWrongGraphKinds(@TempDir Path tmp) throws Exception {
        for(String content:List.of("2 1 wrong\n0 1 1", "2 1 directed\n0 2 1", "2 1 directed\n0", "2 0 directed\nextra")) {
            Path file=tmp.resolve("bad.txt"); Files.writeString(file,content);
            assertThrows(Exception.class,()->Graph.read(file));
        }
        assertThrows(IllegalArgumentException.class,()->MST.prim(new Graph(2,true),new Metrics()));
        assertThrows(IllegalArgumentException.class,()->Kosaraju.run(new Graph(2,false),new Metrics()));
        assertThrows(IllegalArgumentException.class,()->new DSU(2,new Metrics()).find(2));
    }
}