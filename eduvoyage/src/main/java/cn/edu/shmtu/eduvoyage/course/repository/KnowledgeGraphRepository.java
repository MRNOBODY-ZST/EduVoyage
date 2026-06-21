package cn.edu.shmtu.eduvoyage.course.repository;

import cn.edu.shmtu.eduvoyage.course.domain.KnowledgeGraph;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface KnowledgeGraphRepository extends ReactiveCrudRepository<KnowledgeGraph, Long> {

    @Query("SELECT * FROM knowledge_graph WHERE id = :id AND deleted = 0")
    Mono<KnowledgeGraph> findActiveById(Long id);

    /** The (first) default graph of a course; the course module provisions one per course. */
    @Query("SELECT * FROM knowledge_graph WHERE course_id = :courseId AND deleted = 0 ORDER BY id LIMIT 1")
    Mono<KnowledgeGraph> findByCourseId(Long courseId);
}
