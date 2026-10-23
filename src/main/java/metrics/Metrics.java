package metrics;
public final class Metrics {
    public long findCalls, successfulUnions, edgeScans, comparisons;
    public int maxDepth;
    public long dsuOps() { return findCalls + successfulUnions; }
    public void depth(int size) { maxDepth = Math.max(maxDepth, size); }
    public String counters() {
        return dsuOps()+","+edgeScans+","+comparisons+","+maxDepth;
    }
}