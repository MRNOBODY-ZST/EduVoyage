package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeNode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface KnowledgeNodeRepository extends ReactiveCrudRepository<KnowledgeNode, Long> {

    @Query("SELECT * FROM knowledge_node WHERE id = :id AND deleted = 0")
    Mono<KnowledgeNode> findActiveById(Long id);

    @Query("SELECT * FROM knowledge_node WHERE course_id = :courseId AND deleted = 0 ORDER BY id")
    Flux<KnowledgeNode> findByCourseId(Long courseId);

    @Query("SELECT * FROM knowledge_node WHERE chapter_id = :chapterId AND deleted = 0 ORDER BY id")
    Flux<KnowledgeNode> findByChapterId(Long chapterId);

    @Query("SELECT * FROM knowledge_node WHERE graph_id = :graphId AND deleted = 0 ORDER BY id")
    Flux<KnowledgeNode> findByGraphId(Long graphId);

    @Query("SELECT COUNT(*) FROM knowledge_node WHERE chapter_id = :chapterId AND deleted = 0")
    Mono<Long> countByChapterId(Long chapterId);
}
