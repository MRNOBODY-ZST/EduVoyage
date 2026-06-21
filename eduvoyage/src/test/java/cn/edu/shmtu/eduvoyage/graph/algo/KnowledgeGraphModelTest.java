package cn.edu.shmtu.eduvoyage.graph.algo;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure {@link KnowledgeGraphModel} algorithms. No Spring, no
 * I/O — just adjacency in, conclusions out. The fixture graph is:
 *
 * <pre>
 *   1 → 2 → 4
 *   1 → 3 → 4
 *   5 (isolated)
 * </pre>
 *
 * so 1 precedes everything except 5, and 4 depends on both 2 and 3.
 */
class KnowledgeGraphModelTest {

    private static KnowledgeGraphModel diamond() {
        return KnowledgeGraphModel.of(
                List.of(1L, 2L, 3L, 4L, 5L),
                List.of(new long[]{1, 2}, new long[]{1, 3}, new long[]{2, 4}, new long[]{3, 4}));
    }

    @Test
    void topologicalOrderRespectsPrerequisitesAndIsStable() {
        List<Long> order = diamond().topologicalOrder();
        assertThat(order).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
        // 1 before 2,3,4; 2 and 3 before 4
        assertThat(order.indexOf(1L)).isLessThan(order.indexOf(2L));
        assertThat(order.indexOf(1L)).isLessThan(order.indexOf(3L));
        assertThat(order.indexOf(2L)).isLessThan(order.indexOf(4L));
        assertThat(order.indexOf(3L)).isLessThan(order.indexOf(4L));
        // tie-break by id makes the order deterministic; 5 is isolated (indegree 0)
        // but only emitted once the queue drains the chain ahead of it
        assertThat(order).isEqualTo(List.of(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    void cycleIsDetectedAndTopoOrderIsNull() {
        KnowledgeGraphModel cyclic = KnowledgeGraphModel.of(
                List.of(1L, 2L, 3L),
                List.of(new long[]{1, 2}, new long[]{2, 3}, new long[]{3, 1}));
        assertThat(cyclic.hasCycle()).isTrue();
        assertThat(cyclic.topologicalOrder()).isNull();
    }

    @Test
    void wouldCreateCycleFlagsBackEdgesAndSelfLoops() {
        KnowledgeGraphModel m = diamond();
        // 4 already reaches nothing back to 1, so 4→1 closes the loop
        assertThat(m.wouldCreateCycle(4L, 1L)).isTrue();
        // self loop
        assertThat(m.wouldCreateCycle(2L, 2L)).isTrue();
        // a fresh forward edge 1→4 is fine (no cycle)
        assertThat(m.wouldCreateCycle(1L, 4L)).isFalse();
        // linking the isolated node in is fine
        assertThat(m.wouldCreateCycle(4L, 5L)).isFalse();
    }

    @Test
    void prerequisiteChainCollectsTransitivePredecessorsInOrder() {
        List<Long> chain = diamond().prerequisiteChain(4L);
        // 4 needs 1,2,3 (not itself, not 5)
        assertThat(chain).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(chain.indexOf(1L)).isLessThan(chain.indexOf(2L));
        assertThat(chain.indexOf(1L)).isLessThan(chain.indexOf(3L));
        // a root has no prerequisites
        assertThat(diamond().prerequisiteChain(1L)).isEmpty();
    }

    @Test
    void learnableNodesRequireAllDirectPrerequisitesMastered() {
        KnowledgeGraphModel m = diamond();
        // nothing mastered → only roots 1 and 5 are learnable
        assertThat(m.learnableNodes(Set.of())).containsExactly(1L, 5L);
        // mastered 1 → 2 and 3 unlock (5 still open), 4 still blocked
        assertThat(m.learnableNodes(Set.of(1L))).containsExactly(2L, 3L, 5L);
        // mastered 1,2,3 → 4 unlocks
        assertThat(m.learnableNodes(Set.of(1L, 2L, 3L))).containsExactly(4L, 5L);
    }

    @Test
    void recommendedPathDropsMasteredNodesButKeepsOrder() {
        KnowledgeGraphModel m = diamond();
        List<Long> path = m.recommendedPath(Set.of(1L, 5L));
        assertThat(path).containsExactly(2L, 3L, 4L);
        // fully mastered → empty path
        assertThat(m.recommendedPath(Set.of(1L, 2L, 3L, 4L, 5L))).isEmpty();
    }

    @Test
    void recommendedPathIsNullWhenGraphIsCyclic() {
        KnowledgeGraphModel cyclic = KnowledgeGraphModel.of(
                List.of(1L, 2L),
                List.of(new long[]{1, 2}, new long[]{2, 1}));
        assertThat(cyclic.recommendedPath(Set.of())).isNull();
    }

    @Test
    void isReachableWalksTransitively() {
        KnowledgeGraphModel m = diamond();
        assertThat(m.isReachable(1L, 4L)).isTrue();
        assertThat(m.isReachable(4L, 1L)).isFalse();
        assertThat(m.isReachable(5L, 4L)).isFalse();
    }
}
