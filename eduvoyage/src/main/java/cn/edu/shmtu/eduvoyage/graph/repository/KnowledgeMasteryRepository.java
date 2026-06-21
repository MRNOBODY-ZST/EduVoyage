package cn.edu.shmtu.eduvoyage.graph.repository;

import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeMastery;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgeMasteryRepository extends ReactiveCrudRepository<KnowledgeMastery, Long> {

    @Query("SELECT * FROM knowledge_mastery WHERE student_id = :studentId AND node_id = :nodeId AND deleted = 0")
    Mono<KnowledgeMastery> findByStudentAndNode(Long studentId, Long nodeId);

    /** All mastery rows for a student across a set of nodes (the nodes of one graph). */
    @Query("""
            SELECT m.* FROM knowledge_mastery m
            JOIN knowledge_node n ON n.id = m.node_id
            WHERE m.student_id = :studentId AND n.graph_id = :graphId
              AND m.deleted = 0 AND n.deleted = 0
            """)
    Flux<KnowledgeMastery> findByStudentAndGraph(Long studentId, Long graphId);
}
