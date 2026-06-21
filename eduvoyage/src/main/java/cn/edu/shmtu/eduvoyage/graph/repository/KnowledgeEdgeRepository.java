package cn.edu.shmtu.eduvoyage.graph.repository;

import cn.edu.shmtu.eduvoyage.graph.domain.KnowledgeEdge;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgeEdgeRepository extends ReactiveCrudRepository<KnowledgeEdge, Long> {

    @Query("SELECT * FROM knowledge_edge WHERE id = :id AND deleted = 0")
    Mono<KnowledgeEdge> findActiveById(Long id);

    @Query("SELECT * FROM knowledge_edge WHERE graph_id = :graphId AND deleted = 0 ORDER BY id")
    Flux<KnowledgeEdge> findByGraphId(Long graphId);

    @Query("""
            SELECT * FROM knowledge_edge
            WHERE graph_id = :graphId AND type = :type AND deleted = 0
            ORDER BY id
            """)
    Flux<KnowledgeEdge> findByGraphIdAndType(Long graphId, String type);

    /** An existing (active) edge with the same endpoints and type, if any. */
    @Query("""
            SELECT * FROM knowledge_edge
            WHERE from_id = :fromId AND to_id = :toId AND type = :type AND deleted = 0
            LIMIT 1
            """)
    Mono<KnowledgeEdge> findActiveEdge(Long fromId, Long toId, String type);
}
