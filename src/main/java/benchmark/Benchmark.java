package benchmark;
import algorithms.*;
import graph.Graph;
import metrics.Metrics;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import java.util.function.Function;
public final class Benchmark {
    private static final int[] SIZES={1000,3162,10000,31623,100000};
    private static final long[][] REF={
            {147970,27,396274},{30363,1,252411},{468911,137,1271911},{97589,1,723157},
            {1486139,373,3941828},{309550,1,2381342},{4715515,1188,12551891},{968517,1,7376612},
            {15012094,3923,40096539},{3056681,1,23512087}};
    private record Run<T>(T value,Metrics metrics,double ms) {}
    private static <T> Run<T> measure(Function<Metrics,T> action) {
        long[] times=new long[5]; T value=null; Metrics first=null;
        for(int i=0;i<5;i++) {
            Metrics metrics=new Metrics(); long start=System.nanoTime();
            value=action.apply(metrics); times[i]=System.nanoTime()-start;
            if(first==null) first=metrics;
            else check(first.counters().equals(metrics.counters()),"Non-deterministic counters");
        }
        Arrays.sort(times); return new Run<>(value,first,times[2]/1e6);
    }
    private static void check(boolean condition,String message) {
        if(!condition) throw new IllegalStateException(message);
    }
    private static void row(PrintWriter out,String algorithm,String graph,Graph g,Run<?> r,long result) {
        out.printf(Locale.ROOT,"%s,%s,%d,%d,%.6f,%s,%d%n",algorithm,graph,g.n(),g.m(),r.ms(),r.metrics().counters(),result);
    }
    public static void main(String[] args) throws IOException {
        Path dir=Path.of(args.length==0?"results":args[0]); Files.createDirectories(dir);
        Path temporary=dir.resolve("results.csv.tmp"), bonusTemp=dir.resolve("bonus.csv.tmp");
        try(PrintWriter out=new PrintWriter(Files.newBufferedWriter(temporary));
            PrintWriter bonus=new PrintWriter(Files.newBufferedWriter(bonusTemp))) {
            String header="algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result";
            out.println(header); bonus.println(header); int index=0;
            for(int n:SIZES) for(int density:new int[]{4,20}) {
                int m=n*density; String label=density==4?"sparse":"dense";
                Graph undirected=Generator.generate(n,m,Generator.Kind.UNDIRECTED);
                Run<MST.Result> kr=measure(x->MST.kruskal(undirected,x));
                Run<MST.Result> pr=measure(x->MST.prim(undirected,x));
                check(kr.value().weight()==pr.value().weight(),"Kruskal / Prim disagree");
                check(kr.value().edges().size()==n-1 && pr.value().edges().size()==n-1,"Wrong tree size");
                check(kr.value().components()==1 && pr.value().components()==1,"Wrong MST components");
                check(kr.value().weight()==REF[index][0],"MST reference mismatch n="+n+" m="+m+" actual="+kr.value().weight());
                row(out,"Kruskal",label,undirected,kr,kr.value().weight());
                row(out,"Prim",label,undirected,pr,pr.value().weight());
                Graph directed=Generator.generate(n,m,Generator.Kind.DIRECTED);
                Run<Kosaraju.Result> ko=measure(x->Kosaraju.run(directed,x));
                Run<Kosaraju.Result> ta=measure(x->Tarjan.run(directed,x));
                check(samePartition(ko.value(),ta.value()),"Tarjan / Kosaraju disagree");
                check(ko.metrics().edgeScans==2L*m && ta.metrics().edgeScans==m,"SCC scans mismatch");
                check(ko.value().count()==REF[index][1],"SCC reference mismatch n="+n+" actual="+ko.value().count());
                row(out,"Kosaraju",label,directed,ko,ko.value().count());
                row(bonus,"Tarjan",label,directed,ta,ta.value().count());
                Graph dag=Generator.generate(n,m,Generator.Kind.DAG);
                Run<Kahn.Result> ka=measure(x->Kahn.run(dag,x));
                check(!ka.value().hasCycle() && Kahn.isValid(dag,ka.value().order()),"Invalid topological order");
                Run<DagShortestPath.Result> sp=measure(x->DagShortestPath.run(dag,ka.value().order(),x));
                check(ka.metrics().edgeScans==m && sp.metrics().edgeScans==m,"DAG scans mismatch");
                check(sp.value().sum()==REF[index][2],"DAG reference mismatch n="+n+" actual="+sp.value().sum());
                row(out,"Kahn",label,dag,ka,ka.value().order().length);
                row(out,"DagShortestPath",label,dag,sp,sp.value().sum());
                System.out.printf("%d / %d: MST=%d SCC=%d DAG=%d OK%n",n,m,kr.value().weight(),ko.value().count(),sp.value().sum());
                out.flush(); bonus.flush(); index++;
            }
        }
        Files.move(temporary,dir.resolve("results.csv"),StandardCopyOption.REPLACE_EXISTING);
        Files.move(bonusTemp,dir.resolve("bonus.csv"),StandardCopyOption.REPLACE_EXISTING);
    }
    public static boolean samePartition(Kosaraju.Result a,Kosaraju.Result b) {
        if(a.count()!=b.count() || a.component().length!=b.component().length) return false;
        int[] ab=new int[a.count()],ba=new int[b.count()]; Arrays.fill(ab,-1); Arrays.fill(ba,-1);
        for(int i=0;i<a.component().length;i++) {
            int x=a.component()[i],y=b.component()[i];
            if((ab[x]!=-1 && ab[x]!=y) || (ba[y]!=-1 && ba[y]!=x)) return false;
            ab[x]=y; ba[y]=x;
        }
        return true;
    }
}