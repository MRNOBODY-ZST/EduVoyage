package cn.edu.shmtu.eduvoyage.graph.algo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure, side-effect-free graph algorithms over a directed prerequisite graph of
 * knowledge nodes. A directed edge {@code from → to} means "{@code from} is a
 * prerequisite of {@code to}", so a valid study order is any topological order of
 * this graph and a learnable node is one whose every prerequisite is mastered.
 *
 * <p>The class holds nothing but the adjacency structure passed at construction;
 * all node identity is by {@code Long} id. Keeping this layer free of Spring,
 * R2DBC and reactor makes the algorithms exhaustively unit-testable in isolation.</p>
 */
public final class KnowledgeGraphModel {

    /** node id → direct successors (nodes that depend on it). */
    private final Map<Long, Set<Long>> successors = new HashMap<>();
    /** node id → direct predecessors (its prerequisites). */
    private final Map<Long, Set<Long>> predecessors = new HashMap<>();
    private final Set<Long> nodes = new LinkedHashSet<>();

    private KnowledgeGraphModel() {
    }

    /**
     * Builds a model from the full node-id set and the prerequisite edges. Edges
     * referencing unknown nodes are still registered (their endpoints join the
     * node set) so callers do not have to pre-validate, though services normally do.
     *
     * @param nodeIds all node ids in the graph (isolated nodes included)
     * @param edges   prerequisite edges as {@code [fromId, toId]} pairs
     */
    public static KnowledgeGraphModel of(Collection<Long> nodeIds, Collection<long[]> edges) {
        KnowledgeGraphModel m = new KnowledgeGraphModel();
        if (nodeIds != null) {
            for (Long id : nodeIds) {
                m.addNode(id);
            }
        }
        if (edges != null) {
            for (long[] e : edges) {
                m.addEdge(e[0], e[1]);
            }
        }
        return m;
    }

    private void addNode(Long id) {
        if (id == null) {
            return;
        }
        nodes.add(id);
        successors.computeIfAbsent(id, k -> new LinkedHashSet<>());
        predecessors.computeIfAbsent(id, k -> new LinkedHashSet<>());
    }

    private void addEdge(long from, long to) {
        addNode(from);
        addNode(to);
        successors.get(from).add(to);
        predecessors.get(to).add(from);
    }

    public Set<Long> nodes() {
        return Set.copyOf(nodes);
    }

    public Set<Long> directPrerequisites(long nodeId) {
        return Set.copyOf(predecessors.getOrDefault(nodeId, Set.of()));
    }

    // -------------------------------------------------------- reachability

    /**
     * Whether a directed path {@code from → … → to} already exists (BFS over
     * successors). {@code from == to} is treated as reachable (trivial path).
     */
    public boolean isReachable(long from, long to) {
        if (from == to) {
            return true;
        }
        Set<Long> seen = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty()) {
            long cur = queue.poll();
            for (Long next : successors.getOrDefault(cur, Set.of())) {
                if (next == to) {
                    return true;
                }
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    /**
     * Whether adding the prerequisite edge {@code from → to} would create a cycle.
     * That happens iff {@code to} can already reach {@code from} (then the new edge
     * closes the loop), or the edge is a self-loop.
     */
    public boolean wouldCreateCycle(long from, long to) {
        if (from == to) {
            return true;
        }
        return isReachable(to, from);
    }

    /** Whether the current prerequisite graph contains any cycle. */
    public boolean hasCycle() {
        return topologicalOrder() == null;
    }

    // ----------------------------------------------------- topological sort

    /**
     * A topological order of all nodes (Kahn's algorithm), or {@code null} if the
     * graph has a cycle. Ties are broken by ascending id for a stable, repeatable
     * order — important for deterministic tests and UI.
     */
    public List<Long> topologicalOrder() {
        Map<Long, Integer> indegree = new HashMap<>();
        for (Long n : nodes) {
            indegree.put(n, predecessors.getOrDefault(n, Set.of()).size());
        }
        // Min-ordered frontier for stable output.
        List<Long> ready = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                ready.add(e.getKey());
            }
        }
        ready.sort(Long::compareTo);

        List<Long> order = new ArrayList<>(nodes.size());
        while (!ready.isEmpty()) {
            long cur = ready.remove(0);
            order.add(cur);
            for (Long next : successors.getOrDefault(cur, Set.of())) {
                int d = indegree.get(next) - 1;
                indegree.put(next, d);
                if (d == 0) {
                    insertSorted(ready, next);
                }
            }
        }
        return order.size() == nodes.size() ? order : null;
    }

    private static void insertSorted(List<Long> list, long value) {
        int i = 0;
        while (i < list.size() && list.get(i) < value) {
            i++;
        }
        list.add(i, value);
    }

    // ---------------------------------------------------- prerequisite chain

    /**
     * All transitive prerequisites of {@code target} (every node that must be
     * learned before it), returned in study order (a topological order restricted
     * to that set). Excludes {@code target} itself. Empty if it has none.
     */
    public List<Long> prerequisiteChain(long target) {
        Set<Long> required = new HashSet<>();
        Deque<Long> stack = new ArrayDeque<>(predecessors.getOrDefault(target, Set.of()));
        while (!stack.isEmpty()) {
            long cur = stack.pop();
            if (required.add(cur)) {
                stack.addAll(predecessors.getOrDefault(cur, Set.of()));
            }
        }
        List<Long> ordered = new ArrayList<>();
        List<Long> topo = topologicalOrder();
        if (topo == null) {
            // Cyclic graph: fall back to id order so callers still get the set.
            ordered.addAll(required);
            ordered.sort(Long::compareTo);
            return ordered;
        }
        for (Long n : topo) {
            if (required.contains(n)) {
                ordered.add(n);
            }
        }
        return ordered;
    }

    // ------------------------------------------------------- learning path

    /**
     * Nodes the student may study next given what they've mastered: not yet
     * mastered, but every direct prerequisite is. Ascending id order.
     */
    public List<Long> learnableNodes(Set<Long> mastered) {
        Set<Long> done = mastered == null ? Set.of() : mastered;
        List<Long> result = new ArrayList<>();
        for (Long n : nodes) {
            if (done.contains(n)) {
                continue;
            }
            if (done.containsAll(predecessors.getOrDefault(n, Set.of()))) {
                result.add(n);
            }
        }
        result.sort(Long::compareTo);
        return result;
    }

    /**
     * A recommended study sequence for everything the student has not yet mastered:
     * a topological order with already-mastered nodes removed, so prerequisites
     * always precede dependents. {@code null} if the graph is cyclic.
     */
    public List<Long> recommendedPath(Set<Long> mastered) {
        List<Long> topo = topologicalOrder();
        if (topo == null) {
            return null;
        }
        Set<Long> done = mastered == null ? Set.of() : mastered;
        List<Long> path = new ArrayList<>();
        for (Long n : topo) {
            if (!done.contains(n)) {
                path.add(n);
            }
        }
        return path;
    }
}
