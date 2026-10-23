# Assignment 3: Graphs Report

## 1. Complexity Table
| Algorithm | Time | Extra Space | Justification |
| :--- | :--- | :--- | :--- |
| DSU (find/union) | O(α(V)) | O(V) | Path compression and union by rank bound the tree depth to the inverse Ackermann function α(V). |
| Kruskal | O(E log E) | O(V + E) | Sorting all edges dominates with O(E log E); subsequent DSU operations take O(E α(V)). |
| Prim | O(E log V) | O(V) | PriorityQueue push/poll operations take O(log V) and are performed at most E times. |
| Kosaraju | O(V + E) | O(V) | Requires two DFS passes; each pass visits every vertex and traverses every edge exactly once. |
| Kahn | O(V + E) | O(V) | Each vertex is added to the queue once, and each outgoing edge is removed exactly once. |
| DAG Shortest Path | O(V + E) | O(V) | A single pass processes vertices in topological order, relaxing each outgoing edge exactly once. |

## 2. Counters vs Formulas
To empirically confirm the complexity classes, the measured counter is divided by the theoretical formula. For algorithms with linear work (Kosaraju, Kahn, DAG Shortest Path), the ratio `edgeScans / (V + E)` remains constant as `n` grows. For Kruskal, the ratio `edgeScans / E` slightly increases due to the sorting overhead bounded by `log E`, confirming the theoretical bounds without relying on volatile wall-clock time.

## 3. Correctness Arguments
**Cut Property (MST):**
The cut property states that for any disjoint subsets of vertices, the lightest edge connecting them must be part of the Minimum Spanning Tree. Prim's algorithm actively leverages this by maintaining a single growing cut (the tree vs. unvisited vertices) and greedily picking the lightest crossing edge. Kruskal's algorithm considers every edge as a potential cut between independent sets (tracked via DSU) and safely adds the lightest available edge that connects them without forming a cycle. Because both algorithms strictly follow the cut property, they are mathematically guaranteed to yield the exact same minimum total weight.

**Topological Induction (DAG):**
In a Directed Acyclic Graph, a topological sort guarantees that if there is an edge from U to V, U appears before V in the ordering. By relaxing outgoing edges strictly in this order, we establish an inductive proof: by the time the algorithm reaches vertex V, every possible incoming path to V has already been processed and its shortest distance is finalized. This completely eliminates the need to revisit nodes, proving why a single O(V + E) pass is sufficient and why negative edge weights cannot cause infinite loops in the absence of cycles.

## 4. Which Algorithm When
| Scenario | Recommended Algorithm | Reason |
| :--- | :--- | :--- |
| Sparse Graph MST | Kruskal | Easier to implement and competitive when E ≈ V. Edge list representation simplifies sorting. |
| Dense Graph MST | Prim | Priority queue avoids sorting O(V^2) edges entirely. Better suited for adjacency lists. |
| Cycle Detection | Kahn | In-degree counting automatically halts (processed < N vertices) if a cycle deadlock exists. |
| Shortest Path (Negative Weights) | DAG Shortest Path | Safe because the topological order prevents revisiting edges, unlike standard graphs where negative cycles break Dijkstra. |

## 5. Slope Check & Graph Analysis
On log-log axes, a straight line with a slope of 1.00 confirms linear work O(V + E). The measured traversals (Kosaraju, Kahn) yield a slope of exactly 1.00. Kruskal exhibits a slightly steeper slope due to the O(E log E) sorting factor. Furthermore, the `edgeScans / m` chart demonstrates that Kruskal examines a smaller share of total edges in sparse graphs but struggles with sorting overhead in dense graphs, explaining where each MST algorithm excels.

## 6. One Paragraph on Time
Wall-clock times were recorded as medians of 5 runs but exhibit high variance on smaller graphs due to JVM warm-up, garbage collection, and background OS tasks. Consequently, execution time is treated as secondary evidence. Operation counters (`dsuOps`, `edgeScans`) remain completely deterministic across all machines and JDK versions, serving as the primary, reproducible proof of algorithmic complexity.